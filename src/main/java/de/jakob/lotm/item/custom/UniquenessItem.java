package de.jakob.lotm.item.custom;

import de.jakob.lotm.beyonders.singularity.SingularityData;
import de.jakob.lotm.util.BeyonderData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class UniquenessItem extends Item {

    private final String pathway;

    public UniquenessItem(Properties properties, String pathway) {
        super(properties);
        this.pathway = pathway;
    }

    public String getPathway() {
        return pathway;
    }

    @Override
    public boolean isFoil(@NotNull ItemStack stack) {
        return true;
    }

    // Descrição da Singularidade equipável: Caminho, domínio e efeitos
    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        int color = BeyonderData.pathwayInfos.containsKey(pathway) ? BeyonderData.pathwayInfos.get(pathway).color() : 0xFFFFFF;
        tooltip.add(Component.translatable("lotmcraft.singularity.pathway", Component.translatable("lotm.pathway." + pathway)).withColor(color));
        if (SingularityData.getHolder(stack) == null) return;
        float dominance = SingularityData.getDominance(stack);
        if (dominance >= 1f) {
            tooltip.add(Component.translatable("lotmcraft.singularity.tooltip.dominated").withStyle(ChatFormatting.GOLD));
        } else {
            tooltip.add(Component.translatable("lotmcraft.singularity.tooltip.dominance", (int) (dominance * 100)).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        tooltip.add(Component.translatable("lotmcraft.singularity.effect." + pathway + ".positive").withStyle(ChatFormatting.BLUE));
        if (dominance < 1f) {
            tooltip.add(Component.translatable("lotmcraft.singularity.effect." + pathway + ".negative").withStyle(ChatFormatting.RED));
            tooltip.add(Component.translatable("lotmcraft.singularity.tooltip.sanity").withStyle(ChatFormatting.DARK_RED));
        }
        tooltip.add(Component.translatable("lotmcraft.singularity.tooltip.own").withStyle(ChatFormatting.DARK_GRAY));
    }
}
