package de.jakob.lotm.beyonders.potions;

import de.jakob.lotm.beyonders.abilities.fool.marionettes.ControllingUtils;
import de.jakob.lotm.util.BeyonderData;
import de.jakob.lotm.util.data.PathwayInfos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class BeyonderCharacteristicItem extends Item {
   private final String pathway;
   private final int sequence;

   public BeyonderCharacteristicItem(Properties properties, String pathway, int sequence) {
      super(properties);
      this.pathway = pathway;
      this.sequence = sequence;
   }

   public String getPathway() {
      return this.pathway;
   }

   public int getSequence() {
      return this.sequence;
   }

   @NotNull
   public Component getName(ItemStack stack) {
      return Component.translatable("lotm.beyonder_characteristic.named", new Object[]{Component.translatable("lotm.sequence." + BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().replace("_characteristic", ""))})
         .append(Component.literal(" (").append(Component.translatable("lotm.sequence")).append(Component.literal(" " + this.getSequence() + ")")));
   }

   public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
      entity.setInvulnerable(true);
      entity.setUnlimitedLifetime();
      return false;
   }

   @NotNull
   public InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      if (level.isClientSide) {
         return InteractionResultHolder.success(stack);
      } else if (stack.getItem() instanceof BeyonderCharacteristicItem beChar) {
         if (ControllingUtils.isControlling(player)) {
            return InteractionResultHolder.fail(stack);
         } else {
            int seq = beChar.getSequence();
            String path = beChar.getPathway();
            if (path.equals(BeyonderData.getPathway(player)) && seq >= BeyonderData.getSequence(player)) {
               int[] stacks = BeyonderData.getCharStacks(player);
               if (stacks[seq] >= 0 && seq >= 1 && (double)BeyonderData.getDigestionProgress(player) == 1.0) {
                  BeyonderData.setCharStack(player, stacks[seq] + 1, seq, true);
                  BeyonderData.setDigestionProgress(player, 0.0F);
                  player.setItemInHand(hand, ItemStack.EMPTY);
                  return InteractionResultHolder.success(ItemStack.EMPTY);
               }
            }

            return InteractionResultHolder.fail(stack);
         }
      } else {
         return InteractionResultHolder.fail(stack);
      }
   }
}
