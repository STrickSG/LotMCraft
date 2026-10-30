package de.jakob.lotm.util.mixin;

import de.jakob.lotm.hornacis.HornacisWorld;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.RandomState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Quando o Overworld monta o RandomState dele (antes de gerar qualquer chunk), as funções de relevo dele
 * passam a responder pela Cordilheira de Hornacis. As outras dimensões ficam como sempre.
 */
@Mixin(ChunkMap.class)
public class HornacisChunkMapMixin {
    @Shadow
    @Final
    ServerLevel level;

    @Shadow
    @Final
    private RandomState randomState;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void lotmcraft$hornacis(CallbackInfo ci) {
        if (level.dimension() == Level.OVERWORLD) HornacisWorld.attach(randomState);
    }
}
