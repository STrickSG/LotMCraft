package de.jakob.lotm.hornacis;

import com.mojang.serialization.MapCodec;
import de.jakob.lotm.LOTMCraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Funções de densidade da Cordilheira de Hornacis, usadas nas configurações de ruído do Overworld
 * (data/minecraft/worldgen/noise_settings). Fora da cordilheira devolvem exatamente o valor vanilla,
 * então o resto do mundo não muda. Só valem no Overworld: outra dimensão que use as mesmas configurações
 * (de outro mod, por exemplo) continua vanilla.
 */
public final class HornacisDensity {
    public static final DeferredRegister<MapCodec<? extends DensityFunction>> TYPES =
            DeferredRegister.create(Registries.DENSITY_FUNCTION_TYPE, LOTMCraft.MOD_ID);

    /** Quanto a densidade muda por bloco de altura (parecido com o terreno vanilla perto da superfície) */
    private static final double SLOPE = 0.09;
    private static final double LIMIT = 170;

    static {
        TYPES.register("hornacis_terrain", () -> Terrain.MAP_CODEC);
        TYPES.register("hornacis_slide", () -> Slide.MAP_CODEC);
        TYPES.register("hornacis_solid", () -> Solid.MAP_CODEC);
    }

    private HornacisDensity() {}

    public static void register(IEventBus bus) {
        TYPES.register(bus);
    }

    /**
     * A qual cordilheira as funções de um mundo respondem. Cada dimensão monta a própria cópia das funções
     * (uma por RandomState) e todas as cópias da mesma dimensão dividem um só Binding; só o do Overworld
     * recebe a região (ver {@link HornacisWorld#attach}).
     */
    public static final class Binding {
        volatile HornacisRegion region;
    }

    /** Um Binding por montagem de funções: o RandomState monta tudo com um só Visitor. */
    private static final Map<DensityFunction.Visitor, Binding> BINDINGS = Collections.synchronizedMap(new WeakHashMap<>());

    private static Binding bindingFor(Binding current, DensityFunction.Visitor visitor) {
        return current != null ? current : BINDINGS.computeIfAbsent(visitor, v -> new Binding());
    }

    /** Uma função desta classe (para ligar o Binding da dimensão à região). */
    public interface Bound {
        Binding binding();
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
     * no miolo elas o substituem. A superfície é a altura da coluna, sem ruído 3D: nada de arcos, saliências
     * soltas ou buracos. Abaixo dela a densidade cresce como no vanilla, então as cavernas vanilla (que
     * dependem do sloped_cheese) continuam existindo dentro das montanhas, longe das encostas (ver {@link Solid}).
     */
    public record Terrain(DensityFunction input, Binding binding) implements DensityFunction, Bound {
        public static final MapCodec<Terrain> MAP_CODEC = DensityFunction.HOLDER_HELPER_CODEC.fieldOf("input")
                .xmap(input -> new Terrain(input, null), Terrain::input);
        public static final KeyDispatchDataCodec<Terrain> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

        @Override
        public double compute(FunctionContext ctx) {
            HornacisRegion r = binding == null ? null : binding.region;
            if (r == null) return input.compute(ctx);
            Column c = column(r, ctx.blockX(), ctx.blockZ());
            if (c.mask <= 0) return input.compute(ctx);
            double m = Math.max(-LIMIT, Math.min(LIMIT, SLOPE * (c.height - ctx.blockY())));
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
            return visitor.apply(new Terrain(input.mapAll(visitor), bindingFor(binding, visitor)));
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
    public record Slide(Binding binding) implements DensityFunction, Bound {
        public static final MapCodec<Slide> MAP_CODEC = MapCodec.unit(() -> new Slide(null));
        public static final KeyDispatchDataCodec<Slide> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

        @Override
        public double compute(FunctionContext ctx) {
            double y = ctx.blockY();
            double vanilla = Math.max(0, Math.min(1, (256 - y) / 16));
            HornacisRegion r = binding == null ? null : binding.region;
            if (r == null) return vanilla;
            Column c = column(r, ctx.blockX(), ctx.blockZ());
            if (c.mask <= 0) return vanilla;
            double high = Math.max(0, Math.min(1, (1712 - y) / 16));
            return vanilla + (high - vanilla) * c.mask;
        }

        @Override
        public void fillArray(double[] array, ContextProvider provider) {
            provider.fillAllDirectly(array, this);
        }

        @Override
        public DensityFunction mapAll(Visitor visitor) {
            return visitor.apply(new Slide(bindingFor(binding, visitor)));
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

    /** Até onde (em blocos, para os lados) a casca enxerga a superfície mais baixa em volta */
    private static final int SHELL_REACH = 12;
    /** Espessura mínima da casca abaixo da superfície mais baixa em volta */
    private static final int SHELL = 8;

    /**
     * A casca maciça da cordilheira: no miolo dela, nada é escavado perto da superfície. Uma coluna fica
     * maciça desde a superfície até {@link #SHELL} blocos abaixo do ponto mais baixo da superfície num raio de
     * {@link #SHELL_REACH} blocos. Assim nenhuma caverna, túnel ou entrada abre buraco nas encostas, cristas ou
     * picos (lá a "profundidade" vertical engana: atrás de um paredão fino a montanha parece funda), e mesmo
     * assim as cavernas continuam existindo mais para dentro. Envolve a densidade final inteira.
     */
    public record Solid(DensityFunction input, Binding binding) implements DensityFunction, Bound {
        public static final MapCodec<Solid> MAP_CODEC = DensityFunction.HOLDER_HELPER_CODEC.fieldOf("input")
                .xmap(input -> new Solid(input, null), Solid::input);
        public static final KeyDispatchDataCodec<Solid> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

        @Override
        public double compute(FunctionContext ctx) {
            double v = input.compute(ctx);
            if (v > 0) return v;
            HornacisRegion r = binding == null ? null : binding.region;
            if (r == null) return v;
            int x = ctx.blockX(), y = ctx.blockY(), z = ctx.blockZ();
            Shell shell = SHELL_TABLE.get().at(r, x >> 4, z >> 4);
            if (!shell.any) return v;
            int cell = ((x & 15) >> 2) + ((z & 15) >> 2) * 4;
            float bottom = shell.bottom[cell];
            // fora do miolo puro (borda da cordilheira, terreno misturado com o vanilla): cavernas como no vanilla
            if (bottom == Float.NEGATIVE_INFINITY || y < bottom - 12) return v;
            // superfície exata do terreno sem cavernas: a densidade é interpolada entre os cantos das células
            // de 4 blocos, então a superfície é a interpolação linear das alturas desses cantos
            int i = (x & 15) >> 2, j = (z & 15) >> 2;
            double fx = (x & 3) / 4.0, fz = (z & 3) / 4.0;
            double top = (shell.corner(i, j) * (1 - fx) + shell.corner(i + 1, j) * fx) * (1 - fz)
                    + (shell.corner(i, j + 1) * (1 - fx) + shell.corner(i + 1, j + 1) * fx) * fz;
            if (y >= top) return v;
            // logo abaixo da casca as cavernas afinam até sumir (sem teto reto)
            return Math.max(v, Math.min(0.2, (y - bottom) * 0.04));
        }

        @Override
        public void fillArray(double[] array, ContextProvider provider) {
            provider.fillAllDirectly(array, this);
        }

        @Override
        public DensityFunction mapAll(Visitor visitor) {
            return visitor.apply(new Solid(input.mapAll(visitor), bindingFor(binding, visitor)));
        }

        @Override
        public double minValue() {
            return input.minValue();
        }

        @Override
        public double maxValue() {
            return Math.max(input.maxValue(), 0.2);
        }

        @Override
        public KeyDispatchDataCodec<? extends DensityFunction> codec() {
            return CODEC;
        }
    }

    /**
     * A casca de um chunk: alturas nos cantos das células de 4 blocos (as mesmas que o relevo usa), numa
     * janela que passa {@link #SHELL_REACH} blocos para cada lado, e o fundo da casca em cada célula.
     */
    private static final class Shell {
        private static final int PAD = SHELL_REACH / 4;
        /** Cantos por lado: 4 células do chunk + 1, mais a folga dos dois lados */
        private static final int SIDE = 5 + 2 * PAD;

        HornacisRegion region;
        int cx = Integer.MIN_VALUE, cz = Integer.MIN_VALUE;
        boolean any;
        final float[] heights = new float[SIDE * SIDE];
        final boolean[] solidCore = new boolean[SIDE * SIDE];
        /** Fundo da casca em cada uma das 16 células do chunk (-infinito: sem casca ali) */
        final float[] bottom = new float[16];
        final HornacisRegion.Sample sample = new HornacisRegion.Sample();

        double corner(int i, int j) {
            return heights[(i + PAD) + (j + PAD) * SIDE];
        }

        Shell at(HornacisRegion r, int chunkX, int chunkZ) {
            if (r == region && chunkX == cx && chunkZ == cz) return this;
            region = r;
            cx = chunkX;
            cz = chunkZ;
            build(r);
            return this;
        }

        private void build(HornacisRegion r) {
            int x0 = cx << 4, z0 = cz << 4;
            any = false;
            if (r.topIn(x0 - SHELL_REACH, z0 - SHELL_REACH, x0 + 15 + SHELL_REACH, z0 + 15 + SHELL_REACH) <= 0) return;
            for (int j = 0; j < SIDE; j++) {
                for (int i = 0; i < SIDE; i++) {
                    r.sample(x0 + (i - PAD) * 4, z0 + (j - PAD) * 4, sample);
                    int k = i + j * SIDE;
                    heights[k] = sample.mask > 0 ? (float) sample.height : Float.NEGATIVE_INFINITY;
                    // só no miolo puro da cordilheira o terreno é exatamente a altura da coluna
                    solidCore[k] = sample.mask >= 1 && HornacisRegion.core(sample) >= 1;
                }
            }
            for (int cj = 0; cj < 4; cj++) {
                for (int ci = 0; ci < 4; ci++) {
                    int c = ci + cj * 4;
                    int a = ci + PAD, b = cj + PAD;
                    boolean core = solidCore[a + b * SIDE] && solidCore[a + 1 + b * SIDE]
                            && solidCore[a + (b + 1) * SIDE] && solidCore[a + 1 + (b + 1) * SIDE];
                    if (!core) {
                        bottom[c] = Float.NEGATIVE_INFINITY;
                        continue;
                    }
                    // o mais baixo da superfície em volta (a interpolação linear só tem extremos nos cantos)
                    float min = Float.POSITIVE_INFINITY;
                    for (int jj = b - PAD; jj <= b + 1 + PAD; jj++)
                        for (int ii = a - PAD; ii <= a + 1 + PAD; ii++)
                            min = Math.min(min, heights[ii + jj * SIDE]);
                    bottom[c] = min - SHELL;
                    if (bottom[c] != Float.NEGATIVE_INFINITY) any = true;
                }
            }
        }
    }

    private static final ThreadLocal<Shell> SHELL_TABLE = ThreadLocal.withInitial(Shell::new);

    private static final Map<NoiseGeneratorSettings, Boolean> USES = Collections.synchronizedMap(new IdentityHashMap<>());

    /** Estas configurações de ruído têm o relevo de Hornacis? (Overworld e Biomas Grandes) */
    public static boolean isHornacis(NoiseGeneratorSettings settings) {
        return USES.computeIfAbsent(settings, s -> {
            boolean[] found = {false};
            DensityFunction.Visitor visitor = f -> {
                if (f instanceof Bound) found[0] = true;
                return f;
            };
            s.noiseRouter().finalDensity().mapAll(visitor);
            BINDINGS.remove(visitor);
            return found[0];
        });
    }

    /** Liga todas as funções desta montagem (de um RandomState) à região. */
    static void bind(DensityFunction root, HornacisRegion region) {
        DensityFunction.Visitor visitor = f -> {
            if (f instanceof Bound b && b.binding() != null) b.binding().region = region;
            return f;
        };
        root.mapAll(visitor);
        BINDINGS.remove(visitor);
    }
}
