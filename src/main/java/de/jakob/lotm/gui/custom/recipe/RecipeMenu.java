package de.jakob.lotm.gui.custom.recipe;

import de.jakob.lotm.gui.ModMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class RecipeMenu extends AbstractContainerMenu {
    public static final int SUPPLEMENTARY_COUNT = 4;
    public static final int MAIN_SLOT = 4;
    public static final int SLOT_COUNT = SUPPLEMENTARY_COUNT + 1;

    public static final int[][] SLOT_POS = {
            {34, 40}, {70, 40}, {106, 40}, {142, 40},   // supplementary 1-4
            {88, 92}                                    // main
    };

    // Painel da receita e painel do ritual (lado a lado quando a Carta da Blasfêmia mostra os dois)
    public static final int PANEL_W = 192, PANEL_H = 144;
    public static final int RITUAL_W = 192, RITUAL_H = 208, GAP = 8;
    public static final int RITUAL_SLOT_Y = 40, RITUAL_SLOT_SPACING = 30;

    /**
     * O que o menu mostra: a receita, o ritual ou os dois.
     * {@code ritualSlots} é quantos itens o ritual pede (-1 quando não mostra o ritual).
     */
    public record Layout(String pathway, int sequence, boolean recipe, int ritualSlots) {
        public static final Layout RECIPE_ONLY = new Layout("", -1, true, -1);

        public boolean ritual() {
            return ritualSlots >= 0;
        }

        public int width() {
            return recipe && ritual() ? PANEL_W + GAP + RITUAL_W : recipe ? PANEL_W : RITUAL_W;
        }

        public int height() {
            return ritual() ? RITUAL_H : PANEL_H;
        }

        /** Onde começa o painel do ritual (x) */
        public int ritualX() {
            return recipe ? PANEL_W + GAP : 0;
        }

        /** Onde começa o painel da receita (y): centralizado ao lado do painel do ritual, que é mais alto */
        public int recipeY() {
            return ritual() ? (RITUAL_H - PANEL_H) / 2 : 0;
        }

        /** x do item {@code index} do ritual, com os itens centralizados no painel */
        public int ritualSlotX(int index) {
            int span = (ritualSlots - 1) * RITUAL_SLOT_SPACING + 16;
            return ritualX() + (RITUAL_W - span) / 2 + index * RITUAL_SLOT_SPACING;
        }

        public void write(FriendlyByteBuf buf) {
            buf.writeUtf(pathway);
            buf.writeInt(sequence);
            buf.writeBoolean(recipe);
            buf.writeInt(ritualSlots);
        }

        public static Layout read(FriendlyByteBuf buf) {
            return new Layout(buf.readUtf(), buf.readInt(), buf.readBoolean(), buf.readInt());
        }
    }

    private final ItemStackHandler itemHandler;
    private final Layout layout;

    public RecipeMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(new ArrayList<>(List.of()), List.of(), containerId, playerInventory,
                extraData != null && extraData.isReadable() ? Layout.read(extraData) : Layout.RECIPE_ONLY);
    }

    public RecipeMenu(List<ItemStack> ingredients, List<ItemStack> materials, int containerId, Inventory playerInventory, Layout layout) {
        super(ModMenuTypes.RECIPE_MENU.get(), containerId);
        this.layout = layout;
        int recipeSlots = layout.recipe() ? SLOT_COUNT : 0;
        int ritualSlots = Math.max(layout.ritualSlots(), 0);
        this.itemHandler = new ItemStackHandler(recipeSlots + ritualSlots) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return false;
            }

            @Override
            public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
                return ItemStack.EMPTY;
            }
        };

        for (int i = 0; i < recipeSlots; i++) {
            this.addSlot(new SlotItemHandler(itemHandler, i, SLOT_POS[i][0], SLOT_POS[i][1] + layout.recipeY()));
        }
        // itens que o ritual pede, em uma fileira no painel do ritual
        for (int i = 0; i < ritualSlots; i++) {
            this.addSlot(new SlotItemHandler(itemHandler, recipeSlots + i, layout.ritualSlotX(i), RITUAL_SLOT_Y));
        }

        for (int i = 0; i < Math.min(ingredients.size(), recipeSlots); i++) {
            itemHandler.setStackInSlot(i, ingredients.get(i).copy());
        }
        for (int i = 0; i < Math.min(materials.size(), ritualSlots); i++) {
            itemHandler.setStackInSlot(recipeSlots + i, materials.get(i).copy());
        }
    }

    public Layout getLayout() {
        return layout;
    }

    @Override
    public @NotNull ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
