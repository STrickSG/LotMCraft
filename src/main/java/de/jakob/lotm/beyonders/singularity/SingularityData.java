package de.jakob.lotm.beyonders.singularity;

import de.jakob.lotm.item.custom.UniquenessItem;
import de.jakob.lotm.util.BeyonderData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.UUID;

/**
 * Dados guardados no próprio item da Singularidade: quem é o dono e quanto ela já foi dominada (0 a 1).
 */
public final class SingularityData {
    private static final String HOLDER = "SingularityHolder";
    private static final String DOMINANCE = "SingularityDominance";

    private SingularityData() {}

    public static boolean isSingularity(ItemStack stack) {
        return stack.getItem() instanceof UniquenessItem;
    }

    public static String pathwayOf(ItemStack stack) {
        return stack.getItem() instanceof UniquenessItem item ? item.getPathway() : "";
    }

    private static CompoundTag tag(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    private static void write(ItemStack stack, CompoundTag tag) {
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    @Nullable
    public static UUID getHolder(ItemStack stack) {
        CompoundTag tag = tag(stack);
        return tag.hasUUID(HOLDER) ? tag.getUUID(HOLDER) : null;
    }

    public static void setHolder(ItemStack stack, UUID holder) {
        CompoundTag tag = tag(stack);
        tag.putUUID(HOLDER, holder);
        write(stack, tag);
    }

    public static float getDominance(ItemStack stack) {
        return Math.max(0f, Math.min(1f, tag(stack).getFloat(DOMINANCE)));
    }

    public static void setDominance(ItemStack stack, float dominance) {
        CompoundTag tag = tag(stack);
        tag.putFloat(DOMINANCE, Math.max(0f, Math.min(1f, dominance)));
        write(stack, tag);
    }

    public static boolean isDominated(ItemStack stack) {
        return getDominance(stack) >= 1f;
    }

    public static boolean isHolder(ItemStack stack, Player player) {
        UUID holder = getHolder(stack);
        return holder != null && holder.equals(player.getUUID());
    }

    /** Singularidade do próprio Caminho do jogador. */
    public static boolean isOwnPathway(ItemStack stack, Player player) {
        return BeyonderData.isBeyonder(player) && pathwayOf(stack).equals(BeyonderData.getPathway(player));
    }

    /** Caminho vizinho (mesmo grupo) do Caminho do jogador. */
    public static boolean isNeighborPathway(String pathway, Player player) {
        if (!BeyonderData.isBeyonder(player)) return false;
        var infos = BeyonderData.pathwayInfos.get(BeyonderData.getPathway(player));
        return infos != null && Arrays.asList(infos.neighboringPathways()).contains(pathway);
    }

    /**
     * Quem pode carregar/equipar: o dono, sendo do mesmo Caminho (Sequência 1 ou 0)
     * ou um Deus (Sequência 0) de um Caminho vizinho.
     */
    public static boolean canUse(ItemStack stack, Player player) {
        if (!isSingularity(stack) || !isHolder(stack, player)) return false;
        if (isOwnPathway(stack, player)) return BeyonderData.getSequence(player) <= 1;
        return BeyonderData.getSequence(player) == 0 && isNeighborPathway(pathwayOf(stack), player);
    }
}
