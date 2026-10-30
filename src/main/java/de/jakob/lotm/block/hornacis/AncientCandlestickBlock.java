package de.jakob.lotm.block.hornacis;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

/**
 * Castiçal de pedra com uma vela de chama amarela que nunca apaga. Em pé no chão, ou preso na parede
 * (WALL = true; FACING é para onde ele aponta, saindo da parede).
 */
public class AncientCandlestickBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<AncientCandlestickBlock> CODEC = simpleCodec(AncientCandlestickBlock::new);
    public static final BooleanProperty WALL = BooleanProperty.create("wall");
    private static final VoxelShape FLOOR = Block.box(5, 0, 5, 11, 14, 11);
    private static final VoxelShape NORTH = Block.box(5, 4, 8, 11, 13, 16);
    private static final VoxelShape SOUTH = Block.box(5, 4, 0, 11, 13, 8);
    private static final VoxelShape WEST = Block.box(8, 4, 5, 16, 13, 11);
    private static final VoxelShape EAST = Block.box(0, 4, 5, 8, 13, 11);

    public AncientCandlestickBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(WALL, false));
    }

    @Override
    protected @NotNull MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, WALL);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction face = ctx.getClickedFace();
        if (face.getAxis().isHorizontal()) return defaultBlockState().setValue(WALL, true).setValue(FACING, face);
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    protected @NotNull VoxelShape getShape(BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext ctx) {
        if (!state.getValue(WALL)) return FLOOR;
        return switch (state.getValue(FACING)) {
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            case EAST -> EAST;
            default -> NORTH;
        };
    }

    @Override
    public void animateTick(BlockState state, @NotNull Level level, BlockPos pos, @NotNull RandomSource random) {
        // a chama sai do pavio, na ponta da vela; na parede a vela fica 2,5 px mais perto da parede
        double x = pos.getX() + 0.5, z = pos.getZ() + 0.5, y = pos.getY() + 13.6 / 16;
        if (state.getValue(WALL)) {
            Direction facing = state.getValue(FACING);
            x -= facing.getStepX() * 2.5 / 16;
            z -= facing.getStepZ() * 2.5 / 16;
            y = pos.getY() + 12.6 / 16;
        }
        level.addParticle(ParticleTypes.SMALL_FLAME, x, y, z, 0, 0, 0);
        if (random.nextFloat() < 0.15f) level.addParticle(ParticleTypes.SMOKE, x, y + 0.05, z, 0, 0, 0);
    }
}
