package de.jakob.lotm.structure;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
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
 */
public class FarRingPlacement extends StructurePlacement {
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
            Codec.intRange(0, 4096).optionalFieldOf("biome_search_radius", 512).forGetter(p -> p.searchRadius)
    ).apply(instance, FarRingPlacement::new));

    // acesso público aos campos protegidos da classe base (para o codec)
    private Vec3i offset() { return locateOffset(); }
    private FrequencyReductionMethod reduction() { return frequencyReductionMethod(); }
    private float freq() { return frequency(); }
    private int saltValue() { return salt(); }
    private Optional<StructurePlacement.ExclusionZone> zone() { return exclusionZone(); }

    private final int count, minDistance, maxDistance, searchRadius;
    private final HolderSet<Biome> biomes;
    /** As posições não mudam num mundo: calculadas uma vez por gerador */
    private final Map<ChunkGeneratorStructureState, List<ChunkPos>> cache = new WeakHashMap<>();

    public FarRingPlacement(Vec3i locateOffset, FrequencyReductionMethod frequencyReductionMethod, float frequency, int salt,
                            Optional<StructurePlacement.ExclusionZone> exclusionZone, int count, int minDistance, int maxDistance,
                            HolderSet<Biome> biomes, int searchRadius) {
        super(locateOffset, frequencyReductionMethod, frequency, salt, exclusionZone);
        this.count = count;
        this.minDistance = minDistance;
        this.maxDistance = Math.max(minDistance, maxDistance);
        this.biomes = biomes;
        this.searchRadius = searchRadius;
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
            // sem o bioma por perto (oceano grande...): tenta mais para fora, no mesmo ângulo
            for (int attempt = 0; attempt < 4 && chosen == null; attempt++) {
                int d = distance + attempt * searchRadius * 2;
                int x = (int) Math.round(Math.cos(angle) * d), z = (int) Math.round(Math.sin(angle) * d);
                Pair<BlockPos, Holder<Biome>> found = state.biomeSource.findBiomeHorizontal(x, 64, z, searchRadius, 16,
                        biomes::contains, random, true, state.randomState().sampler());
                if (found != null) chosen = found.getFirst();
            }
            if (chosen == null) chosen = new BlockPos((int) Math.round(Math.cos(angle) * distance), 64, (int) Math.round(Math.sin(angle) * distance));
            result.add(new ChunkPos(chosen));
        }
        return List.copyOf(result);
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
