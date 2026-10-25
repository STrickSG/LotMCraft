package de.jakob.lotm.item.custom;

import de.jakob.lotm.addons.rituals.Believers;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.ChatFormatting;

import java.util.List;

/**
 * Registro Sagrado da igreja de um jogador: qualquer jogador, de qualquer Caminho, usa num aldeão (clique direito) para registrá-lo como seu fiel;
 * clique direito no ar para se registrar entre os próprios fiéis. O ritual do Anjo Branco conta esses fiéis.
 * A conversão do aldeão fica em Believers, porque o aldeão abriria a tela de troca antes do item.
 */
public class SacredRegisterItem extends Item {
    public SacredRegisterItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) Believers.worshipSelf(serverPlayer);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.lotmcraft.sacred_register.tooltip.1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.lotmcraft.sacred_register.tooltip.2").withStyle(ChatFormatting.GRAY));
    }
}
