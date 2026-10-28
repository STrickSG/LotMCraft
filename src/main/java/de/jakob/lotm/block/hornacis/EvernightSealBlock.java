package de.jakob.lotm.block.hornacis;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * Um dos 9 blocos do Selo da Noite Eterna. PART vai de 0 a 8, da esquerda para a direita e de cima para
 * baixo, olhando para a face do emblema (FACING é para onde essa face aponta).
 */
public class EvernightSealBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<EvernightSealBlock> CODEC = simpleCodec(EvernightSealBlock::new);
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 8);

    public EvernightSealBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(PART, 4));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }
}
