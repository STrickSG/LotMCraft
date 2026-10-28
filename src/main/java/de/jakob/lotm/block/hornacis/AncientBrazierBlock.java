package de.jakob.lotm.block.hornacis;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

/** Braseiro de pedra com a chama amarela que nunca apaga (nas avenidas e no pórtico do Palácio do Pico). */
public class AncientBrazierBlock extends Block {
    public static final MapCodec<AncientBrazierBlock> CODEC = simpleCodec(AncientBrazierBlock::new);
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 15, 15);

    public AncientBrazierBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected @NotNull MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public void animateTick(@NotNull BlockState state, @NotNull Level level, BlockPos pos, @NotNull RandomSource random) {
        double x = pos.getX() + 0.5, y = pos.getY() + 1.0, z = pos.getZ() + 0.5;
        if (random.nextFloat() < 0.6f) {
            level.addParticle(ParticleTypes.FLAME, x + (random.nextDouble() - 0.5) * 0.5, y + random.nextDouble() * 0.4,
                    z + (random.nextDouble() - 0.5) * 0.5, 0, 0.02, 0);
        }
        if (random.nextFloat() < 0.25f) level.addParticle(ParticleTypes.SMOKE, x, y + 0.9, z, 0, 0.04, 0);
        if (random.nextFloat() < 0.04f) level.addParticle(ParticleTypes.LAVA, x, y, z, 0, 0, 0);
    }
}
