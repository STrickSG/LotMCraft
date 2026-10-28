package de.jakob.lotm.util.mixin;

import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.animal.goat.GoatAi;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Cabras não dão cabeçada em cabras. O vanilla só poupa o tipo da cabra comum, então a Cabra Montanhosa
 * Cinzenta de Hornacis levaria (e daria) cabeçadas das outras cabras; aqui vale para qualquer cabra.
 */
@Mixin(GoatAi.class)
public class HornacisGoatAiMixin {
    @Shadow
    @Final
    @Mutable
    private static TargetingConditions RAM_TARGET_CONDITIONS;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void lotmcraft$goatsSpareGoats(CallbackInfo ci) {
        RAM_TARGET_CONDITIONS = TargetingConditions.forCombat()
                .selector(target -> !(target instanceof Goat) && target.level().getWorldBorder().isWithinBounds(target.getBoundingBox()));
    }
}
