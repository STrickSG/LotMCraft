package de.jakob.lotm.beyonders.potions;

import de.jakob.lotm.LOTMCraft;
import de.jakob.lotm.addons.rituals.AdvancementRituals;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Registra um pergaminho de ritual para cada ritual de avanço ligado (ver AdvancementRituals). */
public class RitualRecipeItemHandler {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(LOTMCraft.MOD_ID);

    public static void registerRituals(IEventBus eventBus) {
        for (AdvancementRituals.Ritual ritual : AdvancementRituals.all()) {
            ITEMS.registerItem(ritual.id(),
                    properties -> new RitualRecipeItem(properties, ritual.pathway(), ritual.sequence()),
                    new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
        }
        ITEMS.register(eventBus);
    }
}
