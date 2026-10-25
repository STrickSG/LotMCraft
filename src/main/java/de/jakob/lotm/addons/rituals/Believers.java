package de.jakob.lotm.addons.rituals;

import de.jakob.lotm.LOTMCraft;
import de.jakob.lotm.item.ModItems;
import de.jakob.lotm.util.BeyonderData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingConversionEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Fiéis: aldeões que adoram um jogador (de qualquer Caminho), marcados com o título "Fiel de [jogador]".
 * Cada aldeão só pode adorar uma pessoa, e quem já foi ferido por você nunca vira seu fiel.
 * Só Beyonders de Sequência 3 ou maior (3, 2, 1, 0) podem reunir fiéis.
 * O ritual do Anjo Branco (Sol, Sequência 1) pede 100 fiéis.
 */
@EventBusSubscriber(modid = LOTMCraft.MOD_ID)
public final class Believers {
    public static final int NEEDED = 100;
    /** Sequência mínima para reunir fiéis: 3 ou maior (número menor) */
    public static final int MAX_SEQUENCE = 3;
    private static final String BELIEVER_OF = "LotmBelieverOf", OLD_NAME = "LotmNameBeforeFaith", WRONGED_BY = "LotmWrongedBy", SELF = "LotmSelfBeliever";
    private static final int COLOR = 0xFFE29A;

    private Believers() {}

    /** Quantos fiéis o jogador tem, contando ele mesmo se já se incluiu */
    public static int count(ServerPlayer player) {
        return data(player.server).of(player.getUUID()).size() + (worshipsSelf(player) ? 1 : 0);
    }

    /** Sanidade mínima com 100 fiéis ou mais: os fiéis são âncoras e seguram o portador antes de perder o controle */
    public static final float ANCHOR_FLOOR = 0.20f;

    /**
     * Âncoras: Beyonders de Sequência 3 ou maior com fiéis não deixam a sanidade cair abaixo deste valor.
     * Cresce com os fiéis, até ANCHOR_FLOOR com 100 ou mais. 0 para quem não tem fiéis ou não pode tê-los.
     */
    public static float anchorFloor(net.minecraft.world.entity.LivingEntity entity) {
        if (!(entity instanceof ServerPlayer player) || !BeyonderData.isBeyonder(player) || BeyonderData.getSequence(player) > MAX_SEQUENCE) return 0f;
        return ANCHOR_FLOOR * Math.min(count(player), NEEDED) / NEEDED;
    }

    public static boolean worshipsSelf(Player player) {
        return player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getBoolean(SELF);
    }

    /** Só Beyonders de Sequência 3 ou maior podem reunir fiéis; os outros recebem um aviso. */
    private static boolean canGather(ServerPlayer player) {
        if (BeyonderData.isBeyonder(player) && BeyonderData.getSequence(player) <= MAX_SEQUENCE) return true;
        player.displayClientMessage(Component.translatable("lotmcraft.believers.not_worthy", MAX_SEQUENCE).withColor(0xFF7A7A), true);
        return false;
    }

    /** Clique direito no ar com o Registro Sagrado: o jogador se registra entre os próprios fiéis */
    public static void worshipSelf(ServerPlayer player) {
        if (!canGather(player)) return;
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if (!persisted.getBoolean(SELF)) {
            persisted.putBoolean(SELF, true);
            player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
            player.serverLevel().sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY(1.1), player.getZ(), 25, 0.4, 0.6, 0.4, 0.05);
            player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 1.2f);
            player.displayClientMessage(Component.translatable("lotmcraft.believers.self", count(player), NEEDED).withColor(COLOR), true);
        } else {
            player.displayClientMessage(Component.translatable("lotmcraft.believers.count", count(player), NEEDED).withColor(COLOR), true);
        }
    }

    // Clique direito num aldeão com o Registro Sagrado: ele vira seu fiel (antes que a tela de troca abra)
    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (!event.getItemStack().is(ModItems.SACRED_REGISTER.get()) || !(event.getTarget() instanceof Villager villager)) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
        if (!(event.getEntity() instanceof ServerPlayer player) || !canGather(player)) return;

        CompoundTag tag = villager.getPersistentData();
        if (tag.hasUUID(BELIEVER_OF)) {
            boolean mine = tag.getUUID(BELIEVER_OF).equals(player.getUUID());
            player.displayClientMessage(Component.translatable(mine ? "lotmcraft.believers.already_yours" : "lotmcraft.believers.other_god").withColor(COLOR), true);
            return;
        }
        if (wronged(tag).contains(player.getUUID())) {
            villager.playSound(SoundEvents.VILLAGER_NO, 1f, 1f);
            player.displayClientMessage(Component.translatable("lotmcraft.believers.wronged").withColor(0xFF7A7A), true);
            return;
        }

        tag.putUUID(BELIEVER_OF, player.getUUID());
        giveTitle(villager, player);
        data(player.server).add(player.getUUID(), villager.getUUID());
        ServerLevel level = player.serverLevel();
        level.sendParticles(ParticleTypes.END_ROD, villager.getX(), villager.getY(1.1), villager.getZ(), 20, 0.3, 0.5, 0.3, 0.04);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, villager.getX(), villager.getY(1.0), villager.getZ(), 8, 0.4, 0.4, 0.4, 0);
        villager.playSound(SoundEvents.VILLAGER_CELEBRATE, 1f, 1f);
        player.displayClientMessage(Component.translatable("lotmcraft.believers.converted", count(player), NEEDED).withColor(COLOR), true);
    }

    // Ferir um aldeão: ele perde a fé em você (se era seu fiel) e nunca mais aceita você
    @SubscribeEvent
    public static void onHurt(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Villager villager) || !(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        CompoundTag tag = villager.getPersistentData();
        ListTag wronged = tag.getList(WRONGED_BY, Tag.TAG_INT_ARRAY);
        if (!wronged(tag).contains(player.getUUID())) wronged.add(NbtUtils.createUUID(player.getUUID()));
        tag.put(WRONGED_BY, wronged);
        if (tag.hasUUID(BELIEVER_OF) && tag.getUUID(BELIEVER_OF).equals(player.getUUID())) {
            tag.remove(BELIEVER_OF);
            removeTitle(villager);
            data(player.server).remove(player.getUUID(), villager.getUUID());
            player.displayClientMessage(Component.translatable("lotmcraft.believers.lost", count(player), NEEDED).withColor(0xFF7A7A), true);
        }
    }

    // Fiel morto ou transformado em aldeão zumbi deixa de contar
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof Villager villager) forget(villager);
    }

    @SubscribeEvent
    public static void onConvert(LivingConversionEvent.Post event) {
        if (!(event.getEntity() instanceof Villager villager) || !villager.getPersistentData().hasUUID(BELIEVER_OF)) return;
        forget(villager);
        event.getOutcome().setCustomName(villager.getCustomName());
    }

    private static void forget(Villager villager) {
        CompoundTag tag = villager.getPersistentData();
        if (!tag.hasUUID(BELIEVER_OF) || villager.getServer() == null) return;
        data(villager.getServer()).remove(tag.getUUID(BELIEVER_OF), villager.getUUID());
        tag.remove(BELIEVER_OF);
        removeTitle(villager);
    }

    // título "Fiel de [jogador]" no nome; um nome dado com etiqueta fica antes do título e volta se a fé acabar
    private static void giveTitle(Villager villager, ServerPlayer player) {
        Component title = Component.translatable("lotmcraft.believers.title", player.getName().getString()).withStyle(ChatFormatting.GOLD);
        Component old = villager.getCustomName();
        if (old != null) {
            villager.getPersistentData().putString(OLD_NAME, Component.Serializer.toJson(old, villager.registryAccess()));
            villager.setCustomName(Component.empty().append(old).append(" ").append(Component.literal("(").append(title).append(")").withStyle(ChatFormatting.GOLD)));
        } else {
            villager.setCustomName(title);
        }
    }

    private static void removeTitle(Villager villager) {
        CompoundTag tag = villager.getPersistentData();
        villager.setCustomName(tag.contains(OLD_NAME) ? Component.Serializer.fromJson(tag.getString(OLD_NAME), villager.registryAccess()) : null);
        tag.remove(OLD_NAME);
    }

    private static Set<UUID> wronged(CompoundTag tag) {
        Set<UUID> result = new HashSet<>();
        for (Tag uuid : tag.getList(WRONGED_BY, Tag.TAG_INT_ARRAY)) result.add(NbtUtils.loadUUID(uuid));
        return result;
    }

    private static Data data(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(Data::new, Data::load, null), "lotmcraft_believers");
    }

    /** Fiéis de cada jogador, salvos no mundo (o aldeão pode estar num chunk descarregado) */
    private static final class Data extends SavedData {
        private final Map<UUID, Set<UUID>> believers = new HashMap<>();

        Set<UUID> of(UUID god) {
            return believers.getOrDefault(god, Set.of());
        }

        void add(UUID god, UUID villager) {
            believers.computeIfAbsent(god, k -> new HashSet<>()).add(villager);
            setDirty();
        }

        void remove(UUID god, UUID villager) {
            Set<UUID> set = believers.get(god);
            if (set != null && set.remove(villager)) setDirty();
        }

        static Data load(CompoundTag tag, HolderLookup.Provider provider) {
            Data data = new Data();
            for (String god : tag.getAllKeys()) {
                Set<UUID> set = new HashSet<>();
                for (Tag uuid : tag.getList(god, Tag.TAG_INT_ARRAY)) set.add(NbtUtils.loadUUID(uuid));
                data.believers.put(UUID.fromString(god), set);
            }
            return data;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
            believers.forEach((god, set) -> {
                ListTag list = new ListTag();
                set.forEach(uuid -> list.add(NbtUtils.createUUID(uuid)));
                tag.put(god.toString(), list);
            });
            return tag;
        }
    }
}
