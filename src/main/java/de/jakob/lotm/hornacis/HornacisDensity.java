package de.jakob.lotm.hornacis;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.jakob.lotm.LOTMCraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Funções de densidade da Cordilheira de Hornacis, usadas nas configurações de ruído do Overworld
 * (data/minecraft/worldgen/noise_settings). Fora da cordilheira devolvem exatamente o valor vanilla,
 * então o resto do mundo não muda.
 */
public final class HornacisDensity {
    public static final DeferredRegister<MapCodec<? extends DensityFunction>> TYPES =
            DeferredRegister.create(Registries.DENSITY_FUNCTION_TYPE, LOTMCraft.MOD_ID);

    /** Quanto a densidade muda por bloco de altura (parecido com o terreno vanilla perto da superfície) */
    private static final double SLOPE = 0.09;
    /** Força do detalhe 3D das rochas (saliências e reentrâncias de poucos blocos) */
    private static final double DETAIL = 0.55;
    private static final double LIMIT = 170;

    static {
        TYPES.register("hornacis_terrain", () -> Terrain.MAP_CODEC);
        TYPES.register("hornacis_slide", () -> Slide.MAP_CODEC);
    }

    private HornacisDensity() {}

    public static void register(IEventBus bus) {
        TYPES.register(bus);
    }

    /** Altura e máscara da última coluna pedida por esta thread (cada coluna é pedida várias vezes seguidas). */
    private static final class Column {
        HornacisRegion region;
        int x = Integer.MIN_VALUE, z = Integer.MIN_VALUE;
        double mask, height, core;
        final HornacisRegion.Sample sample = new HornacisRegion.Sample();

        void at(HornacisRegion r, int bx, int bz) {
            if (r == region && bx == x && bz == z) return;
            region = r;
            x = bx;
            z = bz;
            r.sample(bx, bz, sample);
            mask = sample.mask;
            height = sample.height;
            core = mask > 0 ? HornacisRegion.core(sample) : 0;
        }
    }

    private static final ThreadLocal<Column> COLUMN = ThreadLocal.withInitial(Column::new);

    private static Column column(HornacisRegion r, int x, int z) {
        Column c = COLUMN.get();
        c.at(r, x, z);
        return c;
    }

    /**
     * O relevo: na borda da cordilheira as montanhas se somam ao terreno vanilla (o que for mais alto),
     * no miolo elas o substituem. Abaixo da superfície a densidade cresce como no vanilla, então as cavernas
     * vanilla (que dependem do sloped_cheese) continuam aparecendo dentro das montanhas.
     */
    public record Terrain(DensityFunction input, DensityFunction detail, boolean smooth) implements DensityFunction {
        public static final MapCodec<Terrain> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                DensityFunction.HOLDER_HELPER_CODEC.fieldOf("input").forGetter(Terrain::input),
                DensityFunction.HOLDER_HELPER_CODEC.optionalFieldOf("detail", DensityFunctions.zero()).forGetter(Terrain::detail),
                Codec.BOOL.optionalFieldOf("smooth", false).forGetter(Terrain::smooth)
        ).apply(i, Terrain::new));
        public static final KeyDispatchDataCodec<Terrain> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

        @Override
        public double compute(FunctionContext ctx) {
            HornacisRegion r = HornacisWorld.region();
            if (r == null) return input.compute(ctx);
            Column c = column(r, ctx.blockX(), ctx.blockZ());
            if (c.mask <= 0) return input.compute(ctx);
            double m = SLOPE * (c.height - ctx.blockY());
            if (!smooth) m += DETAIL * detail.compute(ctx);
            m = Math.max(-LIMIT, Math.min(LIMIT, m));
            if (c.mask >= 1 && c.core >= 1) return m;
            double v = input.compute(ctx);
            double union = v + (Math.max(v, m) - v) * c.mask;
            return union + (m - union) * c.core;
        }

        @Override
        public void fillArray(double[] array, ContextProvider provider) {
            provider.fillAllDirectly(array, this);
        }

        @Override
        public DensityFunction mapAll(Visitor visitor) {
            return visitor.apply(new Terrain(input.mapAll(visitor), detail.mapAll(visitor), smooth));
        }

        @Override
        public double minValue() {
            return Math.min(input.minValue(), -LIMIT);
        }

        @Override
        public double maxValue() {
            return Math.max(input.maxValue(), LIMIT);
        }

        @Override
        public KeyDispatchDataCodec<? extends DensityFunction> codec() {
            return CODEC;
        }
    }

    /**
     * O “teto” do terreno: no vanilla nada passa de 256; dentro da cordilheira o teto sobe até perto do
     * limite do mundo (1727).
     */
    public record Slide() implements DensityFunction.SimpleFunction {
        public static final Slide INSTANCE = new Slide();
        public static final MapCodec<Slide> MAP_CODEC = MapCodec.unit(INSTANCE);
        public static final KeyDispatchDataCodec<Slide> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

        @Override
        public double compute(FunctionContext ctx) {
            double y = ctx.blockY();
            double vanilla = Math.max(0, Math.min(1, (256 - y) / 16));
            HornacisRegion r = HornacisWorld.region();
            if (r == null) return vanilla;
            Column c = column(r, ctx.blockX(), ctx.blockZ());
            if (c.mask <= 0) return vanilla;
            double high = Math.max(0, Math.min(1, (1712 - y) / 16));
            return vanilla + (high - vanilla) * c.mask;
        }

        @Override
        public double minValue() {
            return 0;
        }

        @Override
        public double maxValue() {
            return 1;
        }

        @Override
        public KeyDispatchDataCodec<? extends DensityFunction> codec() {
            return CODEC;
        }
    }

    private static final Map<NoiseGeneratorSettings, Boolean> USES = Collections.synchronizedMap(new IdentityHashMap<>());

    /** Estas configurações de ruído têm o relevo de Hornacis? (Overworld e Biomas Grandes) */
    public static boolean isHornacis(NoiseGeneratorSettings settings) {
        return USES.computeIfAbsent(settings, s -> {
            boolean[] found = {false};
            s.noiseRouter().finalDensity().mapAll(f -> {
                if (f instanceof Terrain || f instanceof Slide) found[0] = true;
                return f;
            });
            return found[0];
        });
    }
}
