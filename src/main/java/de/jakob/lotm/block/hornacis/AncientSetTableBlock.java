package de.jakob.lotm.block.hornacis;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

/** Mesa de madeira antiga ainda posta, como se os moradores tivessem saído no meio da refeição. */
public class AncientSetTableBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<AncientSetTableBlock> CODEC = simpleCodec(AncientSetTableBlock::new);
    private static final VoxelShape SHAPE = Shapes.or(Block.box(0, 13, 0, 16, 15, 16),
            Block.box(1, 0, 1, 3, 13, 3), Block.box(13, 0, 1, 15, 13, 3), Block.box(1, 0, 13, 3, 13, 15), Block.box(13, 0, 13, 15, 13, 15));

    public AncientSetTableBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected @NotNull MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    protected @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext ctx) {
        return SHAPE;
    }
}
