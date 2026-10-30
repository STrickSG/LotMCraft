package de.jakob.lotm.beyonders.singularity;

import de.jakob.lotm.LOTMCraft;
import de.jakob.lotm.attachments.ModAttachments;
import de.jakob.lotm.attachments.MultiplierModifierComponent;
import de.jakob.lotm.compat.curios.CuriosCompat;
import de.jakob.lotm.util.BeyonderData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Singularidades equipáveis (Curios):
 * - Sequência 1 do mesmo Caminho: o domínio sobe aos poucos enquanto equipada (1 hora), cobrando 1% de Sanidade
 *   a cada 10 segundos; os bônus do mod (SingularityBonuses) valem pela metade até dominar.
 * - Sequência 0 do mesmo Caminho: já dominada, bônus inteiros.
 * - Sequência 0 de Caminho vizinho: o domínio sobe aos poucos enquanto equipada (2 horas e meia), cobrando 1% de
 *   Sanidade a cada 5 segundos; recebe só efeitos do Minecraft, porque não domina o conceito daquele Caminho.
 * Se a Sanidade zerar, ela rejeita o jogador e perde parte do domínio.
 * Ao morrer, o jogador perde as Singularidades não dominadas, que voltam a poder se manifestar nos templos.
 */
@EventBusSubscriber(modid = LOTMCraft.MOD_ID)
public class SingularityHandler {
    public static final String MULTIPLIER_CAUSE = "singularity";
    public static final float OWN_PATHWAY_MULTIPLIER = 1.25f;
    /** Segundos equipada para dominar a Singularidade do próprio Caminho (1 hora). */
    public static final int OWN_DOMINATION_SECONDS = 60 * 60;
    /** Segundos equipada para dominar a Singularidade de um Caminho vizinho (2 horas e meia). */
    public static final int NEIGHBOR_DOMINATION_SECONDS = 150 * 60;
    private static final String KEPT_TAG = "lotmcraft_kept_singularities";

    private static Boolean curiosLoaded;

    public static boolean curiosLoaded() {
        if (curiosLoaded == null) curiosLoaded = ModList.get().isLoaded("curios");
        return curiosLoaded;
    }

    public static List<ItemStack> equipped(LivingEntity entity) {
        return curiosLoaded() ? CuriosCompat.findEquippedSingularities(entity) : List.of();
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.tickCount % 20 != 0) return; // uma vez por segundo

        boolean ownPathwayBoost = false;
        for (ItemStack stack : equipped(player)) {
            if (!SingularityData.canUse(stack, player)) continue;
            String pathway = SingularityData.pathwayOf(stack);
            SingularityEffects.Definition definition = SingularityEffects.get(pathway);
            boolean own = SingularityData.isOwnPathway(stack, player);
            int sequence = BeyonderData.getSequence(player);

            // Um Deus do próprio Caminho já tem domínio total sobre ela
            if (own && sequence == 0 && !SingularityData.isDominated(stack)) SingularityData.setDominance(stack, 1f);
            if (own) ownPathwayBoost = true;

            // do mesmo Caminho: bônus do mod (SingularityBonuses); vizinho: efeitos do Minecraft
            boolean modBonuses = own && SingularityBonuses.hasBonuses(pathway);
            if (definition != null && !modBonuses) SingularityEffects.applyPositives(player, definition);
            if ("error".equals(pathway)) revealNearby(player);

            if (SingularityData.isDominated(stack)) continue;

            // Ainda não dominada: efeitos negativos e custo de Sanidade
            if (definition != null && player.tickCount % (definition.negativeIntervalSeconds() * 20) == 0) {
                var neighborNegative = own ? null : SingularityEffects.neighborNegative(pathway);
                (neighborNegative != null ? neighborNegative : definition.negative()).accept(player);
            }
            var sanity = player.getData(ModAttachments.SANITY_COMPONENT);
            int sanityInterval = own ? 10 : 5;
            if (player.tickCount % (sanityInterval * 20) == 0) {
                sanity.increaseSanityAndSyncIgnoreSequence(-0.01f, player);
            }

            {
                float before = SingularityData.getDominance(stack);
                float after = before + 1f / (own ? OWN_DOMINATION_SECONDS : NEIGHBOR_DOMINATION_SECONDS);
                if (sanity.getSanity() <= 0.01f) {
                    // Perdeu o controle: a Singularidade rejeita o portador
                    after = Math.max(0f, before - 0.25f);
                    player.hurt(player.damageSources().magic(), 8f);
                    player.displayClientMessage(Component.translatable("lotmcraft.singularity.rejected").withColor(0xAA2233), false);
                }
                SingularityData.setDominance(stack, after);
                if (after >= 1f) {
                    player.displayClientMessage(Component.translatable("lotmcraft.singularity.dominated", stack.getHoverName())
                            .withColor(BeyonderData.pathwayInfos.get(pathway).color()), false);
                } else if ((int) (before * 10) != (int) (after * 10)) {
                    player.displayClientMessage(Component.translatable("lotmcraft.singularity.progress", stack.getHoverName(), (int) (after * 100)), true);
                }
            }
        }
        updateMultiplier(player, ownPathwayBoost);
        SingularityBonuses.tick(player);
    }

    private static void updateMultiplier(ServerPlayer player, boolean active) {
        MultiplierModifierComponent component = player.getData(ModAttachments.MULTIPLIER_MODIFIER_COMPONENT);
        if (active && !component.hasMultiplier(MULTIPLIER_CAUSE)) {
            component.addMultiplier(MULTIPLIER_CAUSE, OWN_PATHWAY_MULTIPLIER);
        } else if (!active) {
            while (component.hasMultiplier(MULTIPLIER_CAUSE)) component.removeMultiplier(MULTIPLIER_CAUSE);
        }
    }

    // Monóculo de Cristal: percepção espiritual, faz as criaturas próximas brilharem
    private static void revealNearby(ServerPlayer player) {
        for (LivingEntity other : player.serverLevel().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(24),
                e -> e != player && e.isAlive())) {
            other.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0, true, false, false));
        }
    }

    /** Dá a Singularidade em forma de item para o jogador e registra o dono. */
    public static void giveSingularity(ServerPlayer player, String pathway, float dominance) {
        ItemStack stack = SingularityItems.stackFor(pathway);
        if (stack.isEmpty()) return;
        SingularityData.setHolder(stack, player.getUUID());
        SingularityData.setDominance(stack, dominance);
        SingularityRegistry.get(player.server).setHolder(pathway, player.getUUID());
        if (!player.getInventory().add(stack)) player.drop(stack, false);
    }

    /** Ao completar a Apoteose, a Singularidade do próprio Caminho passa a ser totalmente dominada. */
    public static void onApotheosisCompleted(ServerPlayer player) {
        String pathway = BeyonderData.getPathway(player);
        for (ItemStack stack : allCarried(player)) {
            if (SingularityData.pathwayOf(stack).equals(pathway) && SingularityData.isHolder(stack, player)) {
                SingularityData.setDominance(stack, 1f);
                return;
            }
        }
    }

    private static List<ItemStack> allCarried(Player player) {
        List<ItemStack> stacks = new ArrayList<>();
        for (ItemStack stack : player.getInventory().items) if (SingularityData.isSingularity(stack)) stacks.add(stack);
        for (ItemStack stack : player.getInventory().offhand) if (SingularityData.isSingularity(stack)) stacks.add(stack);
        stacks.addAll(equipped(player));
        return stacks;
    }

    // Antes dos drops: as não dominadas voltam ao mundo; a dominada do próprio Caminho fica com o Deus
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.isCanceled()) return;
        ListTag kept = new ListTag();
        List<ItemStack> carried = allCarried(player);
        for (ItemStack stack : carried) {
            String pathway = SingularityData.pathwayOf(stack);
            boolean keep = SingularityData.isHolder(stack, player) && SingularityData.isOwnPathway(stack, player)
                    && BeyonderData.getSequence(player) == 0 && SingularityData.isDominated(stack);
            if (keep) {
                kept.add(stack.save(player.registryAccess()));
            } else {
                SingularityRegistry registry = SingularityRegistry.get(player.server);
                // Libera o registro: ela volta a se manifestar nos Templos da Unicidade, como no mod original
                if (player.getUUID().equals(registry.getHolder(pathway))) registry.clear(pathway);
            }
        }
        if (!carried.isEmpty()) {
            player.getInventory().items.replaceAll(s -> SingularityData.isSingularity(s) ? ItemStack.EMPTY : s);
            player.getInventory().offhand.replaceAll(s -> SingularityData.isSingularity(s) ? ItemStack.EMPTY : s);
            if (curiosLoaded()) CuriosCompat.removeEquippedSingularities(player);
        }
        if (!kept.isEmpty()) player.getPersistentData().put(KEPT_TAG, kept);
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath() || !(event.getEntity() instanceof ServerPlayer player)) return;
        CompoundTag data = event.getOriginal().getPersistentData();
        if (!data.contains(KEPT_TAG)) return;
        ListTag kept = data.getList(KEPT_TAG, Tag.TAG_COMPOUND);
        for (int i = 0; i < kept.size(); i++) {
            ItemStack.parse(player.registryAccess(), kept.getCompound(i)).ifPresent(stack -> {
                if (!player.getInventory().add(stack)) player.drop(stack, false);
            });
        }
        data.remove(KEPT_TAG);
    }

    // A Singularidade não pode ser jogada fora
    @SubscribeEvent
    public static void onToss(ItemTossEvent event) {
        ItemStack stack = event.getEntity().getItem();
        if (!SingularityData.isSingularity(stack) || event.getPlayer().level().isClientSide) return;
        event.setCanceled(true);
        if (!event.getPlayer().getInventory().add(stack.copy())) return;
        event.getPlayer().displayClientMessage(Component.translatable("lotmcraft.singularity.cannot_drop"), true);
    }

    // Se por algum motivo virar item no chão (baú quebrado etc.), ela não some nem queima
    @SubscribeEvent
    public static void onItemEntity(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof ItemEntity item && SingularityData.isSingularity(item.getItem())) {
            item.setUnlimitedLifetime();
            item.setInvulnerable(true);
        }
    }
}
