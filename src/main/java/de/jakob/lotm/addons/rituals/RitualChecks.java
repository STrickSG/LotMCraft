package de.jakob.lotm.addons.rituals;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;

import java.util.HashSet;
import java.util.Set;

/** Rituais de avanço que são feitos na hora de beber a poção. */
public final class RitualChecks {
    private RitualChecks() {}

    public static boolean completedWhileDrinking(ServerPlayer player, String pathway, int sequence) {
        // Sol, Sequência 5 (Sacerdote da Luz): beber a poção em escuridão total, enterrado em gelo compactado
        if ("sun".equals(pathway) && sequence == 5) return buriedInPackedIceInDarkness(player);
        return false;
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
            // enterrado: tudo encostado no corpo é gelo compactado
            for (Direction direction : Direction.values()) {
                BlockPos next = pos.relative(direction);
                if (!body.contains(next) && !level.getBlockState(next).is(Blocks.PACKED_ICE)) return false;
            }
        }
        return true;
    }
}
