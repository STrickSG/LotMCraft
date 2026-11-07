package de.jakob.lotm.gui.custom.recipe;

import de.jakob.lotm.util.BeyonderData;
import de.jakob.lotm.util.data.PathwayInfos;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class RecipeMenuProvider implements MenuProvider {
   private final List<ItemStack> ingredients;
   private final String pathway;
   private final int sequence;

   public RecipeMenuProvider(List<ItemStack> ingredients, String pathway, int sequence) {
      this.ingredients = ingredients;
      this.pathway = pathway;
      this.sequence = sequence;
   }

   @NotNull
   public Component getDisplayName() {
      return Component.translatable("lotm.potion_recipe.named", new Object[]{Component.translatable("lotm.sequence." + ((PathwayInfos)BeyonderData.pathwayInfos.get(this.pathway)).getRawSequenceName(this.sequence))})
         .withStyle(new ChatFormatting[]{ChatFormatting.BOLD, ChatFormatting.WHITE});
   }

   public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
      return new RecipeMenu(this.ingredients, containerId, playerInventory);
   }
}
