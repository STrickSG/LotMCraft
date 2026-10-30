package de.jakob.lotm.block.custom;

import com.mojang.serialization.MapCodec;
import de.jakob.lotm.block.ModBlockEntities;
import de.jakob.lotm.block.entity.BlasphemyAltarBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Altar onde a Carta da Blasfêmia única de um Caminho se manifesta, flutuando. */
public class BlasphemyAltarBlock extends BaseEntityBlock {
    public static final MapCodec<BlasphemyAltarBlock> CODEC = simpleCodec(BlasphemyAltarBlock::new);
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 12, 14);

    public BlasphemyAltarBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new BlasphemyAltarBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.BLASPHEMY_ALTAR_BE.get(), BlasphemyAltarBlockEntity::serverTick);
    }

    @Override
    protected @NotNull InteractionResult useWithoutItem(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof BlasphemyAltarBlockEntity altar) || !altar.hasCard()) return InteractionResult.PASS;
        if (!level.isClientSide) altar.take(player);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof BlasphemyAltarBlockEntity altar) {
            ItemStack card = altar.releaseCard();
            if (!card.isEmpty()) Block.popResource(level, pos, card);
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    // brilho da carta flutuando
    @Override
    public void animateTick(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull RandomSource random) {
        if (level.getBlockEntity(pos) instanceof BlasphemyAltarBlockEntity altar && altar.hasCard() && random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.ENCHANT, pos.getX() + .5 + (random.nextDouble() - .5) * .8, pos.getY() + 1.3 + random.nextDouble() * .5,
                    pos.getZ() + .5 + (random.nextDouble() - .5) * .8, 0, -0.3, 0);
        }
    }
}
