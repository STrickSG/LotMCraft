package de.jakob.lotm.beyonders.blasphemy;

import de.jakob.lotm.LOTMCraft;
import de.jakob.lotm.network.PacketHandler;
import de.jakob.lotm.network.packets.toServer.OpenRecipeMenuPacket;
import de.jakob.lotm.util.BeyonderData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Card of Blasphemy: holds the formulas of every potion of one pathway, crafted from its nine potion recipes.
 * Right click shows the formula of the selected sequence, sneak + right click changes the sequence, and it
 * works as the recipe of any sequence of its pathway in the Brewing Cauldron.
 * Only one card of each pathway can exist in a world: a second one fades away.
 */
@EventBusSubscriber(modid = LOTMCraft.MOD_ID)
public class BlasphemyCardItem extends Item {
    private static final String SELECTED_KEY = "BlasphemySequence";
    private static final String UID_KEY = "BlasphemyCardUid";

    private final String pathway;

    public BlasphemyCardItem(Properties properties, String pathway) {
        super(properties);
        this.pathway = pathway;
    }

    public String getPathway() {
        return pathway;
    }

    /** Sequences with an implemented recipe in this pathway, from 9 to 1. */
    private List<Integer> availableSequences() {
        return BeyonderData.implementedRecipes.getOrDefault(pathway, List.of());
    }

    public boolean coversSequence(int sequence) {
        return availableSequences().contains(sequence);
    }

    public int getSelectedSequence(ItemStack stack) {
        List<Integer> sequences = availableSequences();
        if (sequences.isEmpty()) return -1;
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        int selected = tag.contains(SELECTED_KEY) ? tag.getInt(SELECTED_KEY) : sequences.getFirst();
        return sequences.contains(selected) ? selected : sequences.getFirst();
    }

    private Component sequenceName(int sequence) {
        String[] names = BeyonderData.pathwayInfos.get(pathway).sequenceNames();
        return Component.translatable("lotm.sequence." + names[sequence]);
    }

    private static UUID uidOf(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.hasUUID(UID_KEY) ? tag.getUUID(UID_KEY) : null;
    }

    /**
     * Server side: a card without identity (just crafted) becomes the card of its pathway if there is none yet;
     * otherwise, and for any card that is not the registered one (copies), it fades away. Returns false if it vanished.
     */
    private boolean checkUnique(ItemStack stack, ServerLevel level, Entity holder) {
        BlasphemyCardRegistry registry = BlasphemyCardRegistry.get(level.getServer());
        UUID uid = uidOf(stack);
        if (uid == null) {
            UUID fresh = UUID.randomUUID();
            if (registry.claim(pathway, fresh)) {
                CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putUUID(UID_KEY, fresh));
                return true;
            }
        } else if (uid.equals(registry.get(pathway))) {
            return true;
        } else if (registry.get(pathway) == null) {
            // the pathway was freed in the meantime (should not happen); this card becomes the genuine one
            registry.claim(pathway, uid);
            return true;
        }
        if (holder instanceof Player player) {
            player.displayClientMessage(Component.translatable("item.lotmcraft.blasphemy_card.already_exists", getName(stack))
                    .withStyle(ChatFormatting.DARK_PURPLE), false);
        }
        stack.setCount(0);
        return false;
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level, @NotNull Entity entity, int slotId, boolean isSelected) {
        if (level instanceof ServerLevel serverLevel && entity.tickCount % 10 == 0) {
            checkUnique(stack, serverLevel, entity);
        }
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        // The only card of its pathway must not despawn; copies dropped on the ground still fade away
        entity.setUnlimitedLifetime();
        if (entity.level() instanceof ServerLevel serverLevel && entity.tickCount % 10 == 0 && !checkUnique(stack, serverLevel, null)) {
            entity.discard();
            return true;
        }
        return false;
    }

    /** The genuine card was destroyed (killed, e.g. by an explosion or cactus, or fell into the void): it can be crafted again. */
    @SubscribeEvent
    public static void onLeave(EntityLeaveLevelEvent event) {
        if (!(event.getEntity() instanceof ItemEntity item) || !(event.getLevel() instanceof ServerLevel level)) return;
        ItemStack stack = item.getItem();
        if (!(stack.getItem() instanceof BlasphemyCardItem card)) return;
        UUID uid = uidOf(stack);
        if (uid == null) return;
        Entity.RemovalReason reason = item.getRemovalReason();
        boolean fellIntoVoid = item.getY() < level.getMinBuildHeight();
        if (reason == Entity.RemovalReason.KILLED || (reason == Entity.RemovalReason.DISCARDED && fellIntoVoid)) {
            BlasphemyCardRegistry.get(level.getServer()).lost(card.getPathway(), uid);
        }
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player, @NotNull InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        int selected = getSelectedSequence(stack);
        if (selected < 0) return InteractionResultHolder.pass(stack);

        if (player.isShiftKeyDown()) {
            // Change the shown sequence (server side only: it is saved on the item and synced)
            if (!level.isClientSide) {
                List<Integer> sequences = availableSequences();
                int next = sequences.get((sequences.indexOf(selected) + 1) % sequences.size());
                CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(SELECTED_KEY, next));
                player.displayClientMessage(Component.translatable("item.lotmcraft.blasphemy_card.selected", next, sequenceName(next))
                        .withColor(BeyonderData.pathwayInfos.get(pathway).color()), true);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        if (level.isClientSide) {
            PacketHandler.sendToServer(new OpenRecipeMenuPacket(selected, pathway));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public @NotNull Component getName(@NotNull ItemStack stack) {
        return Component.translatable("item.lotmcraft.blasphemy_card", Component.translatable("lotm.pathway." + pathway))
                .withColor(BeyonderData.pathwayInfos.get(pathway).color());
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        List<Integer> sequences = availableSequences();
        if (sequences.isEmpty()) return;
        int selected = getSelectedSequence(stack);
        tooltip.add(Component.translatable("item.lotmcraft.blasphemy_card.desc",
                Component.translatable("lotm.pathway." + pathway), sequences.getFirst(), sequences.getLast()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.lotmcraft.blasphemy_card.cauldron").withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.translatable("item.lotmcraft.blasphemy_card.unique").withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("item.lotmcraft.blasphemy_card.showing", selected, sequenceName(selected)).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("item.lotmcraft.blasphemy_card.use").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("item.lotmcraft.blasphemy_card.cycle").withStyle(ChatFormatting.DARK_GRAY));
    }
}
