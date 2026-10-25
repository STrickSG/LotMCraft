package de.jakob.lotm.addons.rituals;

import de.jakob.lotm.LOTMCraft;
import de.jakob.lotm.beyonders.potions.BeyonderCharacteristicItem;
import de.jakob.lotm.item.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.WrittenBookContent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Juramento de Justiça do Mentor da Justiça (Sol, Sequência 3) e a memória ligada a um item de Anjo
 * do Sol no ritual do Buscador da Luz (Sol, Sequência 2).
 * O jogador escreve e assina um livro com a sua justiça e jura por ela agachado com o livro na mão.
 */
@EventBusSubscriber(modid = LOTMCraft.MOD_ID)
public final class JusticeOath {
    /** 3 dias do Minecraft no lugar dos 3 anos da novel */
    public static final long DURATION = 3 * 24000L;
    private static final String OATH_OWNER = "JusticeOathOwner", SWORN = "LotmJusticeSworn", BROKEN = "LotmJusticeBroken";
    private static final String MEMORY_OWNER = "SunMemoryOwner";
    private static final int COLOR = 0xFFE29A;

    private JusticeOath() {}

    private static long now(ServerPlayer player) {
        return player.server.overworld().getDayTime();
    }

    private static CompoundTag persisted(Player player) {
        return player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
    }

    private static void savePersisted(Player player, CompoundTag tag) {
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, tag);
    }

    private static UUID oathOwner(ItemStack stack) {
        if (!stack.is(Items.WRITTEN_BOOK)) return null;
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.hasUUID(OATH_OWNER) ? tag.getUUID(OATH_OWNER) : null;
    }

    /** Livro jurado pelo próprio jogador no inventário */
    private static boolean hasOathBook(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getUUID().equals(oathOwner(player.getInventory().getItem(i)))) return true;
        }
        return false;
    }

    /** Ticks desde o juramento, ou -1 se não jurou ou quebrou o juramento */
    public static long keptFor(ServerPlayer player) {
        CompoundTag tag = persisted(player);
        if (!tag.contains(SWORN) || tag.getBoolean(BROKEN)) return -1;
        return Math.max(0, now(player) - tag.getLong(SWORN));
    }

    /** Sequência 3: jurou, manteve o juramento por 3 dias sem violar e tem o livro consigo */
    public static boolean upheld(ServerPlayer player) {
        return keptFor(player) >= DURATION && hasOathBook(player);
    }

    // Item com autoridade de Anjo ligado ao Sol: Característica do Sol da Sequência 2 ou menor, ou a Singularidade do Sol
    public static boolean isSunAngelItem(ItemStack stack) {
        if (stack.is(ModItems.SUN_UNIQUENESS.get())) return true;
        return stack.getItem() instanceof BeyonderCharacteristicItem characteristic
                && "sun".equals(characteristic.getPathway()) && characteristic.getSequence() <= 2;
    }

    public static UUID memoryOwner(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.hasUUID(MEMORY_OWNER) ? tag.getUUID(MEMORY_OWNER) : null;
    }

    // Agachado + clique direito com o livro assinado: jura (ou mostra o progresso);
    // com um item de Anjo do Sol na outra mão, liga a memória do juramento a ele
    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        ItemStack book = event.getItemStack();
        if (event.getHand() != InteractionHand.MAIN_HAND || !book.is(Items.WRITTEN_BOOK) || !player.isShiftKeyDown()) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(player.level().isClientSide));
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        WrittenBookContent content = book.get(DataComponents.WRITTEN_BOOK_CONTENT);
        if (content == null || !content.author().equals(player.getName().getString())) {
            player.displayClientMessage(Component.translatable("lotmcraft.justice.not_yours").withColor(0xFF7A7A), true);
            return;
        }

        ItemStack offhand = player.getOffhandItem();
        if (isSunAngelItem(offhand)) {
            bindMemory(serverPlayer, book, offhand, content.title().raw());
            return;
        }

        long kept = keptFor(serverPlayer);
        if (player.getUUID().equals(oathOwner(book)) && kept >= 0) {
            player.displayClientMessage(Component.translatable("lotmcraft.justice.progress",
                    Math.min(3, kept / 24000), 3).withColor(COLOR), true);
            return;
        }
        swear(serverPlayer, book);
    }

    private static void swear(ServerPlayer player, ItemStack book) {
        CustomData.update(DataComponents.CUSTOM_DATA, book, tag -> tag.putUUID(OATH_OWNER, player.getUUID()));
        book.set(DataComponents.LORE, new ItemLore(List.of(
                Component.translatable("lotmcraft.justice.lore", player.getName().getString()).withStyle(ChatFormatting.GOLD))));
        CompoundTag tag = persisted(player);
        tag.putLong(SWORN, now(player));
        tag.remove(BROKEN);
        savePersisted(player, tag);

        ServerLevel level = player.serverLevel();
        level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY(1.1), player.getZ(), 30, 0.4, 0.6, 0.4, 0.05);
        level.playSound(null, player.blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 1f, 1.2f);
        player.displayClientMessage(Component.translatable("lotmcraft.justice.sworn").withColor(COLOR), true);
    }

    private static void bindMemory(ServerPlayer player, ItemStack book, ItemStack item, String title) {
        if (!player.getUUID().equals(oathOwner(book))) {
            player.displayClientMessage(Component.translatable("lotmcraft.justice.memory_needs_oath").withColor(0xFF7A7A), true);
            return;
        }
        CustomData.update(DataComponents.CUSTOM_DATA, item, tag -> tag.putUUID(MEMORY_OWNER, player.getUUID()));
        List<Component> lore = new ArrayList<>(item.getOrDefault(DataComponents.LORE, ItemLore.EMPTY).lines());
        lore.add(Component.translatable("lotmcraft.justice.memory_lore", player.getName().getString(), title).withStyle(ChatFormatting.GOLD));
        item.set(DataComponents.LORE, new ItemLore(lore));
        // a memória sai do livro e passa para o item
        if (!player.getAbilities().instabuild) book.shrink(1);

        ServerLevel level = player.serverLevel();
        level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY(1.0), player.getZ(), 40, 0.5, 0.5, 0.5, 0.08);
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1f, 1f);
        player.displayClientMessage(Component.translatable("lotmcraft.justice.memory_bound").withColor(COLOR), true);
    }

    // Matar um inocente quebra o juramento: aldeões, mercadores, golens, ou um jogador que não atacou você
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer killer)) return;
        Entity victim = event.getEntity();
        boolean innocent = victim instanceof AbstractVillager || victim instanceof IronGolem || victim instanceof SnowGolem
                || (victim instanceof Player other && other != killer && other.getLastHurtMob() != killer);
        if (!innocent) return;

        CompoundTag tag = persisted(killer);
        if (!tag.contains(SWORN) || tag.getBoolean(BROKEN)) return;
        tag.putBoolean(BROKEN, true);
        savePersisted(killer, tag);
        killer.displayClientMessage(Component.translatable("lotmcraft.justice.broken").withColor(0xFF7A7A), true);
    }
}
