package de.jakob.lotm.hornacis;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.SurfaceRules;

/**
 * Condição das regras de superfície: a encosta da cordilheira ali é pelo menos tão íngreme quanto
 * {@code min_slope} (blocos de altura por bloco andado; 1 = 45°). O "steep" do vanilla só pega paredões de mais
 * de ~63°, e as faces do Pico Principal e das cristas, de 35° a 60°, ficariam cobertas de neve como um monte de
 * neve; com esta condição elas mostram a rocha, e a neve fica nas partes mais suaves. Calculada uma vez por coluna.
 */
public record HornacisSlopeCondition(double minSlope) implements SurfaceRules.ConditionSource {
    public static final MapCodec<HornacisSlopeCondition> CODEC = Codec.DOUBLE.fieldOf("min_slope")
            .xmap(HornacisSlopeCondition::new, HornacisSlopeCondition::minSlope);
    private static final KeyDispatchDataCodec<HornacisSlopeCondition> DISPATCH = KeyDispatchDataCodec.of(CODEC);

    @Override
    public KeyDispatchDataCodec<? extends SurfaceRules.ConditionSource> codec() {
        return DISPATCH;
    }

    @Override
    public SurfaceRules.Condition apply(SurfaceRules.Context context) {
        return new SurfaceRules.Condition() {
            private int x = Integer.MIN_VALUE, z = Integer.MIN_VALUE;
            private boolean result;

            @Override
            public boolean test() {
                if (context.blockX != x || context.blockZ != z) {
                    x = context.blockX;
                    z = context.blockZ;
                    result = HornacisWorld.slopeAt(x, z) >= minSlope;
                }
                return result;
            }
        };
    }
}
