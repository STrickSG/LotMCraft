package de.jakob.lotm.util.mixin;

import de.jakob.lotm.hornacis.HornacisRegion;
import de.jakob.lotm.hornacis.HornacisSource;
import de.jakob.lotm.hornacis.HornacisWorld;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

/** O bioma de Hornacis entra na lista de biomas possíveis do Overworld (features, estruturas, compassos). */
@Mixin(BiomeSource.class)
public class HornacisPossibleBiomesMixin implements HornacisSource {
    @Unique
    private HornacisRegion lotmcraft$hornacis;
    @Unique
    private Set<Holder<Biome>> lotmcraft$base;
    @Unique
    private Set<Holder<Biome>> lotmcraft$withHornacis;

    @Override
    public HornacisRegion lotmcraft$hornacis() {
        return lotmcraft$hornacis;
    }

    @Override
    public void lotmcraft$setHornacis(HornacisRegion region) {
        lotmcraft$hornacis = region;
    }

    @Inject(method = "possibleBiomes", at = @At("RETURN"), cancellable = true)
    private void lotmcraft$addHornacis(CallbackInfoReturnable<Set<Holder<Biome>>> cir) {
        HornacisRegion region = lotmcraft$hornacis;
        if (region == null || region != HornacisWorld.region()) return;
        Holder<Biome> biome = HornacisWorld.biome();
        Set<Holder<Biome>> base = cir.getReturnValue();
        if (biome == null || base.contains(biome)) return;
        if (lotmcraft$base != base) {
            Set<Holder<Biome>> set = new ObjectLinkedOpenHashSet<>(base);
            set.add(biome);
            lotmcraft$withHornacis = set;
            lotmcraft$base = base;
        }
        cir.setReturnValue(lotmcraft$withHornacis);
    }
}
