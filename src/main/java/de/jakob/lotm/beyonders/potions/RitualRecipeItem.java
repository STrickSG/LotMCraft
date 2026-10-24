package de.jakob.lotm.beyonders.potions;

import de.jakob.lotm.network.PacketHandler;
import de.jakob.lotm.network.packets.toServer.OpenRecipeMenuPacket;
import de.jakob.lotm.util.BeyonderData;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

/** Pergaminho de ritual: como a receita de poção, mas mostra o que o ritual de avanço precisa. */
public class RitualRecipeItem extends Item {
    private final String pathway;
    private final int sequence;

    public RitualRecipeItem(Properties properties, String pathway, int sequence) {
        super(properties);
        this.pathway = pathway;
        this.sequence = sequence;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand usedHand) {
        if (level.isClientSide) {
            PacketHandler.sendToServer(new OpenRecipeMenuPacket(sequence, pathway, OpenRecipeMenuPacket.VIEW_RITUAL));
        }
        return InteractionResultHolder.success(player.getItemInHand(usedHand));
    }

    @Override
    public @NotNull Component getName(@NotNull ItemStack stack) {
        String sequenceName = BeyonderData.pathwayInfos.get(pathway).sequenceNames()[sequence];
        return Component.translatable("lotm.sequence." + sequenceName).append(" ").append(Component.translatable("lotm.advancement_ritual"))
                .append(" (").append(Component.translatable("lotm.sequence")).append(" " + sequence + ")");
    }
}
