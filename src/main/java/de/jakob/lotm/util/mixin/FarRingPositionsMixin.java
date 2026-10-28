package de.jakob.lotm.util.mixin;

import de.jakob.lotm.structure.FarRingPlacement;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/** As posições de um FarRingPlacement (e dos lugares de Hornacis) são as deles, não as dos anéis das Fortalezas. */
@Mixin(ChunkGeneratorStructureState.class)
public class FarRingPositionsMixin {
    @Inject(method = "generateRingPositions", at = @At("HEAD"), cancellable = true)
    private void lotmcraft$farRingPositions(Holder<StructureSet> structureSet, ConcentricRingsStructurePlacement placement,
                                            CallbackInfoReturnable<CompletableFuture<List<ChunkPos>>> cir) {
        if (placement instanceof FarRingPlacement far) {
            ChunkGeneratorStructureState state = (ChunkGeneratorStructureState) (Object) this;
            cir.setReturnValue(CompletableFuture.supplyAsync(() -> far.positions(state), Util.backgroundExecutor()));
        } else if (placement instanceof de.jakob.lotm.hornacis.structure.HornacisSitePlacement site) {
            // os lugares que a Cordilheira de Hornacis escolheu
            cir.setReturnValue(CompletableFuture.completedFuture(site.positions()));
        }
    }
}
