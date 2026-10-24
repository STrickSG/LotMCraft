package de.jakob.lotm.beyonders.singularity;

import de.jakob.lotm.item.ModItems;
import de.jakob.lotm.item.custom.UniquenessItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Acesso aos itens de Singularidade já registrados pelo mod (ModItems). */
public final class SingularityItems {
    private SingularityItems() {}

    public static List<Item> all() {
        return ModItems.ITEMS.getEntries().stream()
                .map(h -> (Item) h.get())
                .filter(i -> i instanceof UniquenessItem)
                .toList();
    }

    public static ItemStack stackFor(String pathway) {
        for (Item item : all()) {
            if (item instanceof UniquenessItem u && u.getPathway().equals(pathway)) return new ItemStack(item);
        }
        return ItemStack.EMPTY;
    }
}
