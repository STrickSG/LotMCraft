package de.jakob.lotm.util.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import de.jakob.lotm.hornacis.HornacisWorld;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** O ruído do terreno só é calculado até onde há montanha (ver HornacisWorld.noiseFor). */
@Mixin(NoiseChunk.class)
public class HornacisNoiseChunkMixin {
    @ModifyExpressionValue(method = "forChunk", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/levelgen/NoiseSettings;clampToHeightAccessor(Lnet/minecraft/world/level/LevelHeightAccessor;)Lnet/minecraft/world/level/levelgen/NoiseSettings;"))
    private static NoiseSettings lotmcraft$top(NoiseSettings clamped, ChunkAccess chunk, RandomState state,
                                               DensityFunctions.BeardifierOrMarker beardifier, NoiseGeneratorSettings settings,
                                               Aquifer.FluidPicker fluidPicker, Blender blender) {
        ChunkPos pos = chunk.getPos();
        return HornacisWorld.noiseFor(clamped, state, pos.getMinBlockX(), pos.getMinBlockZ(), pos.getMaxBlockX(), pos.getMaxBlockZ());
    }
}
