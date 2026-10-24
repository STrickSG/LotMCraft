package de.jakob.lotm.beyonders.potions;

import de.jakob.lotm.network.PacketHandler;
import de.jakob.lotm.network.packets.toServer.OpenRecipeMenuPacket;
import de.jakob.lotm.util.BeyonderData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Carta da Blasfêmia: guarda as fórmulas de todas as poções de um Caminho.
 * Serve no lugar da receita no Caldeirão de Preparo para qualquer Sequência desse Caminho.
 * Clique direito mostra a fórmula da Sequência escolhida; agachado + clique direito troca a Sequência.
 */
public class BlasphemyCardItem extends Item {
    private static final String SELECTED_KEY = "BlasphemySequence";

    private final String pathway;

    public BlasphemyCardItem(Properties properties, String pathway) {
        super(properties);
        this.pathway = pathway;
    }

    public String getPathway() {
        return pathway;
    }

    /** Sequências que têm receita implementada neste Caminho, da 9 para a 1. */
    private List<Integer> availableSequences() {
        return BeyonderData.implementedRecipes.getOrDefault(pathway, List.of());
    }

    public boolean coversSequence(int sequence) {
        return availableSequences().contains(sequence);
    }

    private int getSelectedSequence(ItemStack stack) {
        List<Integer> sequences = availableSequences();
        if (sequences.isEmpty()) return -1;
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        int selected = tag.contains(SELECTED_KEY) ? tag.getInt(SELECTED_KEY) : sequences.getFirst();
        return sequences.contains(selected) ? selected : sequences.getFirst();
    }

    private Component sequenceName(int sequence) {
        String[] names = BeyonderData.pathwayInfos.get(pathway).sequenceNames();
        return Component.translatable("lotm.sequence." + names[sequence]);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player, @NotNull InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        int selected = getSelectedSequence(stack);
        if (selected < 0) return InteractionResultHolder.pass(stack);

        if (player.isShiftKeyDown()) {
            // Troca a Sequência mostrada (só no servidor, que salva no item e sincroniza)
            if (!level.isClientSide) {
                List<Integer> sequences = availableSequences();
                int next = sequences.get((sequences.indexOf(selected) + 1) % sequences.size());
                CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
                tag.putInt(SELECTED_KEY, next);
                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                player.displayClientMessage(Component.translatable("item.lotmcraft.blasphemy_card.selected", next, sequenceName(next))
                        .withColor(BeyonderData.pathwayInfos.get(pathway).color()), true);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        if (level.isClientSide) {
            PacketHandler.sendToServer(new OpenRecipeMenuPacket(selected, pathway, OpenRecipeMenuPacket.VIEW_CARD));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public @NotNull Component getName(@NotNull ItemStack stack) {
        return Component.translatable("item.lotmcraft.blasphemy_card", Component.translatable("lotm.pathway." + pathway))
                .withColor(BeyonderData.pathwayInfos.get(pathway).color());
    }

    @Override
    public boolean isFoil(@NotNull ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        List<Integer> sequences = availableSequences();
        if (sequences.isEmpty()) return;
        int selected = getSelectedSequence(stack);
        tooltip.add(Component.translatable("item.lotmcraft.blasphemy_card.desc",
                Component.translatable("lotm.pathway." + pathway), sequences.getFirst(), sequences.getLast()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.lotmcraft.blasphemy_card.cauldron").withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("item.lotmcraft.blasphemy_card.showing", selected, sequenceName(selected)).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("item.lotmcraft.blasphemy_card.use").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("item.lotmcraft.blasphemy_card.cycle").withStyle(ChatFormatting.DARK_GRAY));
        if (net.neoforged.fml.ModList.get().isLoaded("curios")) {
            tooltip.add(Component.empty());
            tooltip.add(Component.translatable("item.lotmcraft.blasphemy_card.curio_same").withStyle(ChatFormatting.BLUE));
            tooltip.add(Component.translatable("item.lotmcraft.blasphemy_card.curio_other").withStyle(ChatFormatting.RED));
        }
    }
}
