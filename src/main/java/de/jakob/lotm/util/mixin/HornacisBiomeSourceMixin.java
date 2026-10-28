package de.jakob.lotm.util.mixin;

import de.jakob.lotm.hornacis.HornacisRegion;
import de.jakob.lotm.hornacis.HornacisSource;
import de.jakob.lotm.hornacis.HornacisWorld;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Na Cordilheira de Hornacis o bioma é o dela. Vale para a geração, /locate biome e os compassos de bioma.
 * O TerraBlender também responde por este método no começo dele e sempre decide o bioma, então este
 * injetor é aplicado antes (order 900; o padrão é 1000) para rodar primeiro.
 */
@Mixin(MultiNoiseBiomeSource.class)
public class HornacisBiomeSourceMixin {
    @Inject(method = "getNoiseBiome(IIILnet/minecraft/world/level/biome/Climate$Sampler;)Lnet/minecraft/core/Holder;",
            at = @At("HEAD"), cancellable = true, order = 900)
    private void lotmcraft$hornacis(int x, int y, int z, Climate.Sampler sampler, CallbackInfoReturnable<Holder<Biome>> cir) {
        HornacisRegion region = ((HornacisSource) (Object) this).lotmcraft$hornacis();
        if (region == null) return;
        Holder<Biome> biome = HornacisWorld.biomeAt(region, QuartPos.toBlock(x), QuartPos.toBlock(y), QuartPos.toBlock(z));
        if (biome != null) cir.setReturnValue(biome);
    }
}
