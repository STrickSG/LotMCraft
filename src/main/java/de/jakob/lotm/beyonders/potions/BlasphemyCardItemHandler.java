package de.jakob.lotm.beyonders.potions;

import de.jakob.lotm.LOTMCraft;
import de.jakob.lotm.util.BeyonderData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.Random;

/** Registra uma Carta da Blasfêmia para cada Caminho que já tem receitas implementadas. */
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

    public static List<BlasphemyCardItem> getAllCards() {
        return ITEMS.getEntries().stream()
                .map(DeferredHolder::get)
                .filter(i -> i instanceof BlasphemyCardItem)
                .map(i -> (BlasphemyCardItem) i)
                .toList();
    }

    /** Carta da Blasfêmia de um Caminho (vazio se ele não tiver carta). */
    public static ItemStack cardOf(String pathway) {
        return getAllCards().stream().filter(card -> card.getPathway().equals(pathway)).findFirst()
                .map(ItemStack::new).orElse(ItemStack.EMPTY);
    }

    public static ItemStack randomCard(Random random) {
        List<BlasphemyCardItem> cards = getAllCards();
        if (cards.isEmpty()) return ItemStack.EMPTY;
        return new ItemStack(cards.get(random.nextInt(cards.size())));
    }
}
