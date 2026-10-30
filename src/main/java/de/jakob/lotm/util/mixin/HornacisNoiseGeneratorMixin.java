package de.jakob.lotm.util.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import de.jakob.lotm.hornacis.HornacisWorld;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import org.apache.commons.lang3.mutable.MutableObject;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Predicate;

/** Mesmo limite de altura do ruído no preenchimento do chunk e nas consultas de altura (estruturas, spawn). */
@Mixin(NoiseBasedChunkGenerator.class)
public abstract class HornacisNoiseGeneratorMixin {
    private static final String CLAMP = "Lnet/minecraft/world/level/levelgen/NoiseSettings;clampToHeightAccessor(Lnet/minecraft/world/level/LevelHeightAccessor;)Lnet/minecraft/world/level/levelgen/NoiseSettings;";

    @ModifyExpressionValue(method = "fillFromNoise", at = @At(value = "INVOKE", target = CLAMP))
    private NoiseSettings lotmcraft$fillTop(NoiseSettings clamped, Blender blender, RandomState randomState,
                                            StructureManager structureManager, ChunkAccess chunk) {
        ChunkPos pos = chunk.getPos();
        return HornacisWorld.noiseFor(clamped, randomState, pos.getMinBlockX(), pos.getMinBlockZ(), pos.getMaxBlockX(), pos.getMaxBlockZ());
    }

    @ModifyExpressionValue(method = "iterateNoiseColumn", at = @At(value = "INVOKE", target = CLAMP))
    private NoiseSettings lotmcraft$columnTop(NoiseSettings clamped, LevelHeightAccessor level, RandomState random, int x, int z,
                                              MutableObject<NoiseColumn> column, Predicate<BlockState> stoppingState) {
        return HornacisWorld.noiseFor(clamped, random, x, z, x, z);
    }
}
