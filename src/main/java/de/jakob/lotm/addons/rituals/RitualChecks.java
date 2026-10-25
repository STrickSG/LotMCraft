package de.jakob.lotm.addons.rituals;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

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
        // Sol, Sequência 3 (Mentor da Justiça): manter o próprio Juramento de Justiça por 3 dias
        if ("sun".equals(pathway) && sequence == 3) return JusticeOath.upheld(player);
        // Sol, Sequência 2 (Buscador da Luz): item de Anjo do Sol com a sua memória, posto à sua frente
        if ("sun".equals(pathway) && sequence == 2) return memoryPlacedBefore(player);
        // Sol, Sequência 1 (Anjo Branco): ser adorado por 100 fiéis, contando você mesmo
        if ("sun".equals(pathway) && sequence == 1) return Believers.count(player) >= Believers.NEEDED;
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

    /** Item de Anjo do Sol com a memória do jogador, num suporte de item ou no chão à frente dele (até 4 blocos) */
    private static boolean memoryPlacedBefore(ServerPlayer player) {
        Vec3 look = player.getLookAngle().multiply(1, 0, 1).normalize();
        for (Entity entity : player.level().getEntities(player, player.getBoundingBox().inflate(4))) {
            ItemStack stack = entity instanceof ItemFrame frame ? frame.getItem()
                    : entity instanceof ItemEntity item ? item.getItem() : ItemStack.EMPTY;
            if (!JusticeOath.isSunAngelItem(stack) || !player.getUUID().equals(JusticeOath.memoryOwner(stack))) continue;
            Vec3 toItem = entity.position().subtract(player.position()).multiply(1, 0, 1);
            if (toItem.lengthSqr() < 1.0E-4 || toItem.normalize().dot(look) > 0) return true;
        }
        return false;
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
