package de.jakob.lotm.addons.blasphemy;

import de.jakob.lotm.LOTMCraft;
import de.jakob.lotm.beyonders.potions.BlasphemyCardItem;
import de.jakob.lotm.beyonders.potions.BlasphemyCardItemHandler;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;

import java.util.UUID;

/** Identidade da carta verdadeira de cada Caminho e o que acontece quando ela é destruída. */
@EventBusSubscriber(modid = LOTMCraft.MOD_ID)
public final class BlasphemyCards {
    private static final String UID = "BlasphemyCardUid";

    private BlasphemyCards() {}

    /** A carta verdadeira de um Caminho, com a identidade registrada. */
    public static ItemStack trueCard(String pathway, UUID uid) {
        ItemStack card = BlasphemyCardItemHandler.cardOf(pathway);
        if (!card.isEmpty() && uid != null) CustomData.update(DataComponents.CUSTOM_DATA, card, tag -> tag.putUUID(UID, uid));
        return card;
    }

    public static UUID uidOf(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.hasUUID(UID) ? tag.getUUID(UID) : null;
    }

    // A carta no chão não queima nem some; só se perde no vazio (ou com /kill). Aí ela pode reaparecer num altar.
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
}
