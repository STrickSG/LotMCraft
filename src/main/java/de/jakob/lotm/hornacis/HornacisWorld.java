package de.jakob.lotm.hornacis;

import de.jakob.lotm.LOTMCraft;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

/**
 * Liga a Cordilheira de Hornacis ao mundo que está aberto: monta a região pela seed antes de o mundo
 * começar a gerar, marca a fonte de biomas do Overworld e diz até que altura o ruído precisa ser calculado.
 */
@EventBusSubscriber(modid = LOTMCraft.MOD_ID)
public final class HornacisWorld {
    public static final ResourceKey<Biome> BIOME = ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(LOTMCraft.MOD_ID, "hornacis"));
    /** A partir desta máscara a coluna é do bioma de Hornacis (na borda o terreno ainda é quase o vanilla) */
    private static final double BIOME_MASK = 0.45;
    /** Altura até onde o terreno vanilla pode chegar (o ruído vanilla vai até 320) */
    private static final int VANILLA_TOP = 320;

    private static volatile HornacisRegion region;
    private static volatile Holder<Biome> biome;

    private HornacisWorld() {}

    public static HornacisRegion region() {
        return region;
    }

    public static Holder<Biome> biome() {
        return biome;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        region = null;
        biome = null;
        MinecraftServer server = event.getServer();
        RegistryAccess access = server.registryAccess();
        LevelStem stem = access.registryOrThrow(Registries.LEVEL_STEM).get(LevelStem.OVERWORLD);
        if (stem == null || !(stem.generator() instanceof NoiseBasedChunkGenerator generator)) return;
        if (!(generator.getBiomeSource() instanceof MultiNoiseBiomeSource)) return;
        NoiseGeneratorSettings settings = generator.generatorSettings().value();
        // Amplificado, plano, bioma único...: sem cordilheira
        if (!HornacisDensity.isHornacis(settings)) return;
        Holder<Biome> holder = access.registryOrThrow(Registries.BIOME).getHolder(BIOME).orElse(null);
        if (holder == null) return;

        long seed = server.getWorldData().worldGenOptions().seed();
        long start = System.nanoTime();
        // terra firme pela “continentalidade” vanilla (a cordilheira ainda não existe neste cálculo)
        RandomState random = RandomState.create(settings, access.lookupOrThrow(Registries.NOISE), seed);
        DensityFunction continents = random.router().continents();
        HornacisRegion.LandTest land = (x, z) -> continents.compute(new DensityFunction.SinglePointContext(x, 0, z)) > -0.11;
        Path file = server.getWorldPath(LevelResource.ROOT).resolve("data").resolve("lotmcraft_hornacis.bin");
        HornacisRegion created = HornacisRegion.create(seed, land, new FileStore(file));
        biome = holder;
        region = created;
        ((HornacisSource) generator.getBiomeSource()).lotmcraft$setHornacis(created);
        LOTMCraft.LOGGER.info("Cordilheira de Hornacis: centro {} {}, Pico Principal {} {} ({} ms)",
                Math.round(created.centerX), Math.round(created.centerZ), Math.round(created.mainX), Math.round(created.mainZ),
                (System.nanoTime() - start) / 1_000_000);
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        region = null;
        biome = null;
    }

    /**
     * Bioma de Hornacis nesta posição (em blocos) para a fonte de biomas marcada com a região {@code r},
     * ou null para deixar a escolha com o vanilla.
     */
    public static Holder<Biome> biomeAt(HornacisRegion r, int x, int y, int z) {
        Holder<Biome> b = biome;
        if (r != region || b == null || y < 0 || !r.inBox(x, z)) return null;
        return r.mask(x, z, SAMPLE.get()) >= BIOME_MASK ? b : null;
    }

    private static final ThreadLocal<HornacisRegion.Sample> SAMPLE = ThreadLocal.withInitial(HornacisRegion.Sample::new);

    /**
     * Até onde calcular o ruído do terreno num pedaço do mundo. As configurações do Overworld vão até 1727
     * por causa da cordilheira; fora dela o terreno vanilla nunca passa de 256, então o ruído para em 320
     * como sempre (sem custo a mais). Dentro dela, só até o pico mais alto daquele pedaço.
     */
    public static NoiseSettings noiseFor(NoiseSettings clamped, NoiseGeneratorSettings settings, int x0, int z0, int x1, int z1) {
        if (!HornacisDensity.isHornacis(settings)) return clamped;
        int top = VANILLA_TOP;
        HornacisRegion r = region;
        if (r != null) {
            int t = r.topIn(x0 - 8, z0 - 8, x1 + 8, z1 + 8);
            if (t > 0) top = Math.max(VANILLA_TOP, ((t + 48) >> 4 << 4) + 16);
        }
        int max = clamped.minY() + clamped.height();
        if (top >= max) return clamped;
        return new NoiseSettings(clamped.minY(), top - clamped.minY(), clamped.noiseSizeHorizontal(), clamped.noiseSizeVertical());
    }

    /** A grade erodida guardada na pasta do mundo (comprimida, ~1 MB) */
    private record FileStore(Path file) implements HornacisRegion.GridStore {
        private static final int MAGIC = 0x484F524E;

        @Override
        public float[] load(long key, int size) {
            if (!Files.isRegularFile(file)) return null;
            try (DataInputStream in = new DataInputStream(new java.io.BufferedInputStream(new InflaterInputStream(Files.newInputStream(file))))) {
                if (in.readInt() != MAGIC || in.readLong() != key || in.readInt() != size) return null;
                float[] grid = new float[size];
                for (int i = 0; i < size; i++) grid[i] = in.readFloat();
                return grid;
            } catch (IOException e) {
                LOTMCraft.LOGGER.warn("Cordilheira de Hornacis: não deu para ler {}, recalculando", file, e);
                return null;
            }
        }

        @Override
        public void save(long key, float[] grid) {
            try {
                Files.createDirectories(file.getParent());
                Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
                try (DataOutputStream out = new DataOutputStream(new java.io.BufferedOutputStream(new DeflaterOutputStream(Files.newOutputStream(tmp))))) {
                    out.writeInt(MAGIC);
                    out.writeLong(key);
                    out.writeInt(grid.length);
                    for (float f : grid) out.writeFloat(f);
                }
                Files.move(tmp, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                LOTMCraft.LOGGER.warn("Cordilheira de Hornacis: não deu para salvar {}", file, e);
            }
        }
    }
}
