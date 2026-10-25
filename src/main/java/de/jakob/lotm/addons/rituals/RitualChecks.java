package de.jakob.lotm.addons.rituals;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.Set;

/** Rituais de avanço que são feitos na hora de beber a poção. */
public final class RitualChecks {
    private RitualChecks() {}

    public static boolean completedWhileDrinking(ServerPlayer player, String pathway, int sequence) {
        // Sol, Sequência 5 (Sacerdote da Luz): beber a poção em escuridão total, enterrado em gelo que não derrete
        if ("sun".equals(pathway) && sequence == 5) return buriedInPackedIceInDarkness(player);
        // Sol, Sequência 4 (Sem Sombra): as emoções guardadas no frasco voltam durante o avanço
        if ("sun".equals(pathway) && sequence == 4) return returnOwnEmotions(player);
        return false;
    }

    // gelo que não derrete: compactado ou azul (na novel, Derrick usou gelo azul)
    private static boolean isEverlastingIce(BlockState state) {
        return state.is(Blocks.PACKED_ICE) || state.is(Blocks.BLUE_ICE);
    }

    private static boolean buriedInPackedIceInDarkness(ServerPlayer player) {
        Level level = player.level();
        // blocos ocupados pelo corpo do jogador
        Set<BlockPos> body = new HashSet<>();
        BlockPos.betweenClosedStream(player.getBoundingBox().deflate(1.0E-3)).forEach(pos -> body.add(pos.immutable()));
        if (body.isEmpty()) return false;

        for (BlockPos pos : body) {
            // escuridão total: nenhuma luz do céu nem de blocos
            if (level.getBrightness(LightLayer.SKY, pos) > 0 || level.getBrightness(LightLayer.BLOCK, pos) > 0) return false;
            // enterrado: tudo encostado no corpo é gelo que não derrete
            for (Direction direction : Direction.values()) {
                BlockPos next = pos.relative(direction);
                if (!body.contains(next) && !isEverlastingIce(level.getBlockState(next))) return false;
            }
        }
        return true;
    }

    /** Procura no inventário um Frasco de Emoções com as emoções do próprio jogador e o gasta */
    private static boolean returnOwnEmotions(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (player.getUUID().equals(EmotionFlask.ownerOf(stack))) {
                stack.shrink(1);
                player.displayClientMessage(Component.translatable("lotmcraft.ritual.sun.4.returned").withColor(0xFFE29A), true);
                return true;
            }
        }
        return false;
    }
}
