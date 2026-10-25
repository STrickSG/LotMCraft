package de.jakob.lotm.structure;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.QuartPos;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

/**
 * Um número fixo de estruturas por mundo, longe do spawn e longe umas das outras:
 * ficam num anel em volta do spawn (entre min_distance e max_distance blocos), igualmente espaçadas,
 * e cada uma vai para o bioma da lista mais perto daquele ponto.
 * Para o Minecraft ela é um concentric_rings (como as Fortalezas), então o /locate e mods como o
 * Explorer's Compass acham as estruturas; as posições vêm daqui (FarRingPositionsMixin).
 */
public class FarRingPlacement extends ConcentricRingsStructurePlacement {
    public static final MapCodec<FarRingPlacement> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Vec3i.offsetCodec(16).optionalFieldOf("locate_offset", Vec3i.ZERO).forGetter(FarRingPlacement::offset),
            FrequencyReductionMethod.CODEC.optionalFieldOf("frequency_reduction_method", FrequencyReductionMethod.DEFAULT).forGetter(FarRingPlacement::reduction),
            Codec.floatRange(0.0F, 1.0F).optionalFieldOf("frequency", 1.0F).forGetter(FarRingPlacement::freq),
            ExtraCodecs.NON_NEGATIVE_INT.fieldOf("salt").forGetter(FarRingPlacement::saltValue),
            StructurePlacement.ExclusionZone.CODEC.optionalFieldOf("exclusion_zone").forGetter(FarRingPlacement::zone),
            Codec.intRange(1, 64).fieldOf("count").forGetter(p -> p.count),
            Codec.intRange(0, 1_000_000).fieldOf("min_distance").forGetter(p -> p.minDistance),
            Codec.intRange(0, 1_000_000).fieldOf("max_distance").forGetter(p -> p.maxDistance),
            RegistryCodecs.homogeneousList(Registries.BIOME).fieldOf("biomes").forGetter(p -> p.biomes),
            Codec.intRange(0, 4096).optionalFieldOf("biome_search_radius", 512).forGetter(p -> p.searchRadius),
            RegistryCodecs.homogeneousList(Registries.BIOME).optionalFieldOf("preferred_biomes", HolderSet.empty()).forGetter(p -> p.preferred),
            RegistryCodecs.homogeneousList(Registries.STRUCTURE_SET).optionalFieldOf("avoid", HolderSet.empty()).forGetter(p -> p.avoid),
            Codec.intRange(0, 100_000).optionalFieldOf("avoid_distance", 800).forGetter(p -> p.avoidDistance)
    ).apply(instance, FarRingPlacement::new));

    // acesso público aos campos protegidos da classe base (para o codec)
    private Vec3i offset() { return locateOffset(); }
    private FrequencyReductionMethod reduction() { return frequencyReductionMethod(); }
    private float freq() { return frequency(); }
    private int saltValue() { return salt(); }
    private Optional<StructurePlacement.ExclusionZone> zone() { return exclusionZone(); }

    private final int count, minDistance, maxDistance, searchRadius;
    private final HolderSet<Biome> biomes;
    /** Procurados primeiro (ex.: deserto); sem eles por perto, vale qualquer bioma de "biomes" */
    private final HolderSet<Biome> preferred;
    /** Outros conjuntos far_ring dos quais as estruturas deste ficam a pelo menos avoid_distance blocos */
    private final HolderSet<net.minecraft.world.level.levelgen.structure.StructureSet> avoid;
    private final int avoidDistance;
    /** As posições não mudam num mundo: calculadas uma vez por gerador */
    private final Map<ChunkGeneratorStructureState, List<ChunkPos>> cache = new WeakHashMap<>();

    public FarRingPlacement(Vec3i locateOffset, FrequencyReductionMethod frequencyReductionMethod, float frequency, int salt,
                            Optional<StructurePlacement.ExclusionZone> exclusionZone, int count, int minDistance, int maxDistance,
                            HolderSet<Biome> biomes, int searchRadius, HolderSet<Biome> preferred,
                            HolderSet<net.minecraft.world.level.levelgen.structure.StructureSet> avoid, int avoidDistance) {
        super(locateOffset, frequencyReductionMethod, frequency, salt, exclusionZone, 0, 1, count, biomes);
        this.count = count;
        this.minDistance = minDistance;
        this.maxDistance = Math.max(minDistance, maxDistance);
        this.biomes = biomes;
        this.searchRadius = searchRadius;
        this.preferred = preferred;
        this.avoid = avoid;
        this.avoidDistance = avoidDistance;
    }

    /** Os chunks onde as estruturas deste conjunto nascem neste mundo. */
    public List<ChunkPos> positions(ChunkGeneratorStructureState state) {
        synchronized (cache) {
            return cache.computeIfAbsent(state, this::compute);
        }
    }

    private List<ChunkPos> compute(ChunkGeneratorStructureState state) {
        RandomSource random = RandomSource.create(state.getLevelSeed() ^ salt());
        double start = random.nextDouble() * Math.PI * 2;
        List<ChunkPos> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            double angle = start + i * Math.PI * 2 / count;
            // o ponto procurado fica longe o bastante para que o bioma achado nunca fique a menos de min_distance
            int distance = minDistance + searchRadius + random.nextInt(Math.max(1, maxDistance - minDistance - searchRadius) + 1);
            BlockPos chosen = null;
            // perto demais de uma estrutura de "avoid": gira um pouco para um lado e para o outro
            for (int shift = 0; shift < 9 && chosen == null; shift++) {
                double turn = Math.toRadians(((shift + 1) / 2) * 12.0 * (shift % 2 == 1 ? 1 : -1));
                BlockPos candidate = find(state, angle + turn, distance, random);
                if (!tooClose(state, candidate)) chosen = candidate;
            }
            if (chosen == null) chosen = find(state, angle, distance, random);
            result.add(new ChunkPos(chosen));
        }
        return List.copyOf(result);
    }

    private BlockPos find(ChunkGeneratorStructureState state, double angle, int distance, RandomSource random) {
        if (preferred.size() > 0) {
            // com bioma preferido: procura dentro da faixa de distância, em vários pontos e girando até 30° para cada lado;
            // primeiro o preferido (ex.: deserto), depois qualquer um da lista; a faixa só é deixada se não houver nenhum
            int mid = (minDistance + maxDistance) / 2, half = Math.max(0, (maxDistance - minDistance) / 2 - searchRadius);
            for (HolderSet<Biome> wanted : List.of(preferred, biomes)) {
                for (int off : new int[]{0, 10, -10, 20, -20, 30, -30}) {
                    for (int d : new int[]{mid, mid - half, mid + half}) {
                        double a = angle + Math.toRadians(off);
                        int x = (int) Math.round(Math.cos(a) * d), z = (int) Math.round(Math.sin(a) * d);
                        Pair<BlockPos, Holder<Biome>> found = state.biomeSource.findBiomeHorizontal(x, 64, z, searchRadius, 16,
                                wanted::contains, random, true, state.randomState().sampler());
                        if (found == null) continue;
                        BlockPos inside = deeper(state, found.getFirst(), x, z, wanted);
                        if (!tooClose(state, inside)) return inside;
                    }
                }
            }
        }
        // sem o bioma por perto (oceano grande...): tenta mais para fora, no mesmo ângulo
        for (int attempt = 0; attempt < 4; attempt++) {
            int d = distance + attempt * searchRadius * 2;
            int x = (int) Math.round(Math.cos(angle) * d), z = (int) Math.round(Math.sin(angle) * d);
            Pair<BlockPos, Holder<Biome>> found = state.biomeSource.findBiomeHorizontal(x, 64, z, searchRadius, 16,
                    biomes::contains, random, true, state.randomState().sampler());
            if (found != null) return found.getFirst();
        }
        return new BlockPos((int) Math.round(Math.cos(angle) * distance), 64, (int) Math.round(Math.sin(angle) * distance));
    }

    /** O ponto achado fica na borda do bioma (o mais perto da procura): anda um pouco mais para dentro dele */
    private static BlockPos deeper(ChunkGeneratorStructureState state, BlockPos found, int fromX, int fromZ, HolderSet<Biome> wanted) {
        double dx = found.getX() - fromX, dz = found.getZ() - fromZ, len = Math.sqrt(dx * dx + dz * dz);
        if (len < 1) return found;
        for (int step : new int[]{96, 64, 32}) {
            int x = found.getX() + (int) Math.round(dx / len * step), z = found.getZ() + (int) Math.round(dz / len * step);
            if (wanted.contains(state.biomeSource.getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(64), QuartPos.fromBlock(z), state.randomState().sampler())))
                return new BlockPos(x, 64, z);
        }
        return found;
    }

    private boolean tooClose(ChunkGeneratorStructureState state, BlockPos pos) {
        for (var set : avoid) {
            if (!(set.value().placement() instanceof FarRingPlacement other) || other == this) continue;
            for (ChunkPos p : other.positions(state)) {
                double dx = p.getMiddleBlockX() - pos.getX(), dz = p.getMiddleBlockZ() - pos.getZ();
                if (dx * dx + dz * dz < (double) avoidDistance * avoidDistance) return true;
            }
        }
        return false;
    }

    @Override
    protected boolean isPlacementChunk(@NotNull ChunkGeneratorStructureState state, int x, int z) {
        return positions(state).contains(new ChunkPos(x, z));
    }

    @Override
    public @NotNull StructurePlacementType<?> type() {
        return ModStructures.FAR_RING.get();
    }
}
