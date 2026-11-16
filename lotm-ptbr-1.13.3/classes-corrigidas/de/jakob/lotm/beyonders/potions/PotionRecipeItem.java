package de.jakob.lotm.beyonders.potions;

import de.jakob.lotm.network.PacketHandler;
import de.jakob.lotm.network.packets.toServer.OpenRecipeMenuPacket;
import de.jakob.lotm.util.data.PathwayInfos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class PotionRecipeItem extends Item {
   private PotionRecipe recipe = null;

   public PotionRecipeItem(Properties properties) {
      super(properties);
   }

   public PotionRecipe getRecipe() {
      return this.recipe;
   }

   public void setRecipe(PotionRecipe recipe) {
      this.recipe = recipe;
   }

   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
      if (level.isClientSide && this.recipe != null) {
         PacketHandler.sendToServer(new OpenRecipeMenuPacket(this.recipe.potion().getSequence(), this.recipe.potion().getPathway()));
      }

      return InteractionResultHolder.success(player.getItemInHand(usedHand));
   }

   @NotNull
   public Component getName(ItemStack stack) {
      return Component.translatable("lotm.potion_recipe.named", new Object[]{Component.translatable("lotm.sequence." + BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath())})
         .append(
            this.recipe == null
               ? Component.literal("")
               : Component.literal(" (")
                  .append(Component.translatable("lotm.sequence"))
                  .append(Component.literal(" " + this.recipe.potion().getSequence() + ")"))
         );
   }
}
