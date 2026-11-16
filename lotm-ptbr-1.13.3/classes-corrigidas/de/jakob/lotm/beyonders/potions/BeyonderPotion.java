package de.jakob.lotm.beyonders.potions;

import de.jakob.lotm.beyonders.abilities.fool.marionettes.ControllingUtils;
import de.jakob.lotm.util.BeyonderData;
import de.jakob.lotm.util.helper.AdvancementUtil;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class BeyonderPotion extends Item {
   private final int sequence;
   private final String pathway;

   public BeyonderPotion(Properties properties, int sequence, String pathway) {
      super(properties);
      this.sequence = sequence;
      this.pathway = pathway;
   }

   @NotNull
   public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
      if (!level.isClientSide && (!(entity instanceof Player player) || !ControllingUtils.isControlling(player))) {
         AdvancementUtil.advance(entity, this.pathway, this.sequence);
      }

      return new ItemStack((ItemLike)PotionItemHandler.EMPTY_BOTTLE.get());
   }

   public int getUseDuration(ItemStack stack, LivingEntity entity) {
      return 32;
   }

   public UseAnim getUseAnimation(ItemStack stack) {
      return UseAnim.DRINK;
   }

   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      if (AdvancementUtil.isWeakerPotionOfOwnPathway(player, this.pathway, this.sequence)) {
         return InteractionResultHolder.fail(player.getItemInHand(hand));
      } else {
         player.startUsingItem(hand);
         return InteractionResultHolder.consume(player.getItemInHand(hand));
      }
   }

   @NotNull
   public Component getName(ItemStack stack) {
      return Component.translatable("lotm.potion.named", new Object[]{sequenceName(this.pathway, this.sequence)});
   }

   private static Component sequenceName(String pathway, int sequence) {
      de.jakob.lotm.util.data.PathwayInfos infos = (de.jakob.lotm.util.data.PathwayInfos)BeyonderData.pathwayInfos.get(pathway);
      String raw = infos == null ? "" : infos.getRawSequenceName(sequence);
      return (Component)(raw.isEmpty() ? Component.literal(BeyonderData.getSequenceName(pathway, sequence)) : Component.translatable("lotm.sequence." + raw));
   }

   public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
      super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
      tooltipComponents.add(
         Component.translatable("lotm.pathway.named", new Object[]{sequenceName(this.pathway, 9)}).withStyle(ChatFormatting.DARK_GRAY)
      );
      tooltipComponents.add(Component.translatable("lotm.sequence").append(Component.literal(" " + this.sequence)).withStyle(ChatFormatting.DARK_GRAY));
   }

   public int getSequence() {
      return this.sequence;
   }

   public String getPathway() {
      return this.pathway;
   }
}
