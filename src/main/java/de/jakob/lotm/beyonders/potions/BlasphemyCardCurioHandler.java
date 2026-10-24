package de.jakob.lotm.beyonders.potions;

import de.jakob.lotm.LOTMCraft;
import de.jakob.lotm.attachments.ModAttachments;
import de.jakob.lotm.attachments.MultiplierModifierComponent;
import de.jakob.lotm.compat.curios.CuriosCompat;
import de.jakob.lotm.util.BeyonderData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Efeitos da Carta da Blasfêmia equipada num espaço do Curios (como o Klein faz com as cartas):
 * - mesmo Caminho do jogador: as habilidades do Caminho ficam mais fortes;
 * - outro Caminho (ou sem Caminho): reforço físico, mas da Sequência 9 até a 5 a carta
 *   cobra 1% de Sanidade a cada 5 segundos. Da Sequência 4 para cima o corpo já resiste.
 */
@EventBusSubscriber(modid = LOTMCraft.MOD_ID)
public class BlasphemyCardCurioHandler {
    public static final String MULTIPLIER_CAUSE = "blasphemy_card";
    public static final float SAME_PATHWAY_MULTIPLIER = 1.2f;
    private static final float SANITY_COST = 0.01f;
    private static final int SANITY_INTERVAL_TICKS = 100;

    private static final ResourceLocation DAMAGE_ID = ResourceLocation.fromNamespaceAndPath(LOTMCraft.MOD_ID, "blasphemy_card_damage");
    private static final ResourceLocation ARMOR_ID = ResourceLocation.fromNamespaceAndPath(LOTMCraft.MOD_ID, "blasphemy_card_armor");
    private static final ResourceLocation SPEED_ID = ResourceLocation.fromNamespaceAndPath(LOTMCraft.MOD_ID, "blasphemy_card_speed");
    private static final ResourceLocation HEALTH_ID = ResourceLocation.fromNamespaceAndPath(LOTMCraft.MOD_ID, "blasphemy_card_health");

    private static Boolean curiosLoaded;

    private static boolean curiosLoaded() {
        if (curiosLoaded == null) curiosLoaded = ModList.get().isLoaded("curios");
        return curiosLoaded;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.tickCount % 20 != 0) return; // checa uma vez por segundo

        ItemStack card = curiosLoaded() ? CuriosCompat.findEquippedCard(player) : ItemStack.EMPTY;
        String cardPathway = card.getItem() instanceof BlasphemyCardItem c ? c.getPathway() : null;
        boolean samePathway = cardPathway != null && BeyonderData.isBeyonder(player)
                && cardPathway.equals(BeyonderData.getPathway(player));

        updateMultiplier(player, samePathway);
        updatePhysicalBoost(player, cardPathway != null && !samePathway);

        if (cardPathway != null && !samePathway && player.tickCount % SANITY_INTERVAL_TICKS == 0) {
            int sequence = BeyonderData.isBeyonder(player) ? BeyonderData.getSequence(player) : LOTMCraft.NON_BEYONDER_SEQ;
            if (sequence >= 5) {
                player.getData(ModAttachments.SANITY_COMPONENT).increaseSanityAndSyncIgnoreSequence(-SANITY_COST, player);
                if (player.tickCount % (SANITY_INTERVAL_TICKS * 6) == 0) {
                    player.displayClientMessage(Component.translatable("item.lotmcraft.blasphemy_card.backlash").withColor(0xAA2233), true);
                }
            }
        }
    }

    private static void updateMultiplier(ServerPlayer player, boolean active) {
        MultiplierModifierComponent component = player.getData(ModAttachments.MULTIPLIER_MODIFIER_COMPONENT);
        if (active && !component.hasMultiplier(MULTIPLIER_CAUSE)) {
            component.addMultiplier(MULTIPLIER_CAUSE, SAME_PATHWAY_MULTIPLIER);
        } else if (!active) {
            while (component.hasMultiplier(MULTIPLIER_CAUSE)) component.removeMultiplier(MULTIPLIER_CAUSE);
        }
    }

    private static void updatePhysicalBoost(ServerPlayer player, boolean active) {
        apply(player, Attributes.ATTACK_DAMAGE, DAMAGE_ID, 3.0, AttributeModifier.Operation.ADD_VALUE, active);
        apply(player, Attributes.ARMOR, ARMOR_ID, 4.0, AttributeModifier.Operation.ADD_VALUE, active);
        apply(player, Attributes.MOVEMENT_SPEED, SPEED_ID, 0.1, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, active);
        apply(player, Attributes.MAX_HEALTH, HEALTH_ID, 4.0, AttributeModifier.Operation.ADD_VALUE, active);
    }

    private static void apply(ServerPlayer player, Holder<Attribute> attribute, ResourceLocation id, double amount,
                              AttributeModifier.Operation operation, boolean active) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return;
        boolean has = instance.hasModifier(id);
        if (active && !has) {
            instance.addTransientModifier(new AttributeModifier(id, amount, operation));
        } else if (!active && has) {
            instance.removeModifier(id);
            if (attribute == Attributes.MAX_HEALTH && player.getHealth() > player.getMaxHealth()) {
                player.setHealth(player.getMaxHealth());
            }
        }
    }
}
