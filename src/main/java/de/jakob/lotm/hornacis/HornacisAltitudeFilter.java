package de.jakob.lotm.hornacis;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

/**
 * Faixa de altitude de uma vegetação na cordilheira (floresta embaixo, prados mais acima, nada nos picos).
 * Perto dos limites a chance cai aos poucos, então a linha das árvores não fica reta.
 */
public class HornacisAltitudeFilter extends PlacementFilter {
    public static final MapCodec<HornacisAltitudeFilter> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.INT.optionalFieldOf("min_y", -2048).forGetter(f -> f.minY),
            Codec.INT.optionalFieldOf("max_y", 4096).forGetter(f -> f.maxY),
            Codec.intRange(1, 512).optionalFieldOf("fade", 32).forGetter(f -> f.fade)
    ).apply(i, HornacisAltitudeFilter::new));

    private final int minY, maxY, fade;

    public HornacisAltitudeFilter(int minY, int maxY, int fade) {
        this.minY = minY;
        this.maxY = maxY;
        this.fade = fade;
    }

    @Override
    protected boolean shouldPlace(PlacementContext context, RandomSource random, BlockPos pos) {
        int y = pos.getY();
        double chance = 1;
        if (y < minY) chance = 1 - (minY - y) / (double) fade;
        else if (y > maxY) chance = 1 - (y - maxY) / (double) fade;
        return chance >= 1 || (chance > 0 && random.nextDouble() < chance);
    }

    @Override
    public PlacementModifierType<?> type() {
        return HornacisSetup.ALTITUDE.get();
    }
}
