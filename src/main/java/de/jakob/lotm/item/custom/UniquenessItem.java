package de.jakob.lotm.item.custom;

import de.jakob.lotm.beyonders.singularity.SingularityBonuses;
import de.jakob.lotm.beyonders.singularity.client.SingularityTooltipClient;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.loading.FMLEnvironment;
import net.minecraft.locale.Language;

import de.jakob.lotm.beyonders.singularity.SingularityData;
import de.jakob.lotm.util.BeyonderData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
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

    // Singularidades não são armas nem ferramentas comuns: não aceitam encantamentos (mesa, bigorna ou livro)
    @Override
    public boolean isEnchantable(@NotNull ItemStack stack) {
        return false;
    }

    @Override
    public int getEnchantmentValue(@NotNull ItemStack stack) {
        return 0;
    }

    @Override
    public boolean isBookEnchantable(@NotNull ItemStack stack, @NotNull ItemStack book) {
        return false;
    }

    @Override
    public boolean supportsEnchantment(@NotNull ItemStack stack, @NotNull Holder<Enchantment> enchantment) {
        return false;
    }

    @Override
    public boolean isPrimaryItemFor(@NotNull ItemStack stack, @NotNull Holder<Enchantment> enchantment) {
        return false;
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
        // Descrição curta: só o que vale para quem está lendo; segurando Shift, a completa
        if (SingularityBonuses.hasBonuses(pathway) && FMLEnvironment.dist.isClient() && !SingularityTooltipClient.expanded()) {
            shortTooltip(SingularityTooltipClient.viewer(), dominance, tooltip);
            return;
        }
        if (SingularityBonuses.hasBonuses(pathway)) {
            // Mesmo Caminho: bônus do mod; Caminho vizinho: efeitos do Minecraft
            tooltip.add(Component.translatable("lotmcraft.singularity.own_header").withStyle(ChatFormatting.GOLD));
            tooltip.add(Component.translatable("lotmcraft.singularity.power").withStyle(ChatFormatting.BLUE));
            for (int i = 1; Language.getInstance().has("lotmcraft.singularity.bonus." + pathway + "." + i); i++) {
                tooltip.add(Component.translatable("lotmcraft.singularity.bonus." + pathway + "." + i).withStyle(ChatFormatting.BLUE));
            }
            if (dominance < 1f) tooltip.add(Component.translatable("lotmcraft.singularity.half").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("lotmcraft.singularity.neighbor",
                    Component.translatable("lotmcraft.singularity.effect." + pathway + ".positive")).withStyle(ChatFormatting.DARK_AQUA));
            String ability = SingularityBonuses.neighborAbility(pathway);
            if (ability != null) tooltip.add(Component.translatable("lotmcraft.singularity.neighbor_ability",
                    Component.translatable("lotmcraft." + ability)).withStyle(ChatFormatting.DARK_AQUA));
        } else {
            tooltip.add(Component.translatable("lotmcraft.singularity.effect." + pathway + ".positive").withStyle(ChatFormatting.BLUE));
        }
        if (dominance < 1f) {
            tooltip.add(Component.translatable("lotmcraft.singularity.effect." + pathway + ".negative").withStyle(ChatFormatting.RED));
            if (Language.getInstance().has("lotmcraft.singularity.effect." + pathway + ".neighbor_negative"))
                tooltip.add(Component.translatable("lotmcraft.singularity.effect." + pathway + ".neighbor_negative").withStyle(ChatFormatting.RED));
            tooltip.add(Component.translatable("lotmcraft.singularity.tooltip.sanity").withStyle(ChatFormatting.DARK_RED));
        }
        if (!SingularityBonuses.hasBonuses(pathway)) tooltip.add(Component.translatable("lotmcraft.singularity.tooltip.own").withStyle(ChatFormatting.DARK_GRAY));
    }

    private void shortTooltip(Player viewer, float dominance, List<Component> tooltip) {
        boolean own = viewer != null && BeyonderData.isBeyonder(viewer) && pathway.equals(BeyonderData.getPathway(viewer));
        boolean neighbor = viewer != null && SingularityData.isNeighborPathway(pathway, viewer);
        if (own) {
            for (int i = 1; Language.getInstance().has("lotmcraft.singularity.short." + pathway + ".own." + i); i++) {
                tooltip.add(Component.translatable("lotmcraft.singularity.short." + pathway + ".own." + i).withStyle(ChatFormatting.BLUE));
            }
            if (dominance < 1f) tooltip.add(Component.translatable("lotmcraft.singularity.short." + pathway + ".own_negative").withStyle(ChatFormatting.RED));
        } else if (neighbor) {
            tooltip.add(Component.translatable("lotmcraft.singularity.effect." + pathway + ".positive").withStyle(ChatFormatting.DARK_AQUA));
            String ability = SingularityBonuses.neighborAbility(pathway);
            if (ability != null) tooltip.add(Component.translatable("lotmcraft.singularity.short.neighbor_ability",
                    Component.translatable("lotmcraft." + ability)).withStyle(ChatFormatting.DARK_AQUA));
            if (dominance < 1f) tooltip.add(Component.translatable("lotmcraft.singularity.short." + pathway + ".neighbor_negative").withStyle(ChatFormatting.RED));
        }
        tooltip.add(Component.translatable("lotmcraft.singularity.short.shift").withStyle(ChatFormatting.DARK_GRAY));
    }
}
