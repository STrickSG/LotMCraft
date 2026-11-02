package de.jakob.lotm.beyonders.blasphemy;

import de.jakob.lotm.LOTMCraft;
import de.jakob.lotm.util.BeyonderData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Registers one Card of Blasphemy for every pathway that has potion recipes. */
public class BlasphemyCardItemHandler {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(LOTMCraft.MOD_ID);

    public static void registerCards(IEventBus eventBus) {
        for (String pathway : BeyonderData.pathways) {
            if (!BeyonderData.implementedRecipes.containsKey(pathway)) continue;
            ITEMS.registerItem("blasphemy_card_" + pathway,
                    properties -> new BlasphemyCardItem(properties, pathway),
                    new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant());
        }
        ITEMS.register(eventBus);
    }
}
