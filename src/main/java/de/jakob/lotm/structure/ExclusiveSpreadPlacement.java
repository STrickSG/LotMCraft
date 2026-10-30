package de.jakob.lotm.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Igual ao random_spread do Minecraft, mas não nasce perto de outras estruturas da lista "avoid"
 * (o exclusion_zone do Minecraft só aceita uma). Assim as estruturas do mod não nascem umas em cima das outras.
 * Atenção: a lista não pode ter ciclos (A evita B e B evita A).
 */
public class ExclusiveSpreadPlacement extends RandomSpreadStructurePlacement {
    public static final MapCodec<ExclusiveSpreadPlacement> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Vec3i.offsetCodec(16).optionalFieldOf("locate_offset", Vec3i.ZERO).forGetter(ExclusiveSpreadPlacement::offset),
            FrequencyReductionMethod.CODEC.optionalFieldOf("frequency_reduction_method", FrequencyReductionMethod.DEFAULT).forGetter(ExclusiveSpreadPlacement::reduction),
            Codec.floatRange(0.0F, 1.0F).optionalFieldOf("frequency", 1.0F).forGetter(ExclusiveSpreadPlacement::freq),
            ExtraCodecs.NON_NEGATIVE_INT.fieldOf("salt").forGetter(ExclusiveSpreadPlacement::saltValue),
            StructurePlacement.ExclusionZone.CODEC.optionalFieldOf("exclusion_zone").forGetter(ExclusiveSpreadPlacement::zone),
            Codec.intRange(0, 4096).fieldOf("spacing").forGetter(RandomSpreadStructurePlacement::spacing),
            Codec.intRange(0, 4096).fieldOf("separation").forGetter(RandomSpreadStructurePlacement::separation),
            RandomSpreadType.CODEC.optionalFieldOf("spread_type", RandomSpreadType.LINEAR).forGetter(RandomSpreadStructurePlacement::spreadType),
            RegistryCodecs.homogeneousList(Registries.STRUCTURE_SET).fieldOf("avoid").forGetter(p -> p.avoid),
            Codec.intRange(1, 16).optionalFieldOf("avoid_chunks", 6).forGetter(p -> p.avoidChunks)
    ).apply(instance, ExclusiveSpreadPlacement::new));

    // acesso público aos campos protegidos da classe base (para o codec)
    private Vec3i offset() { return locateOffset(); }
    private FrequencyReductionMethod reduction() { return frequencyReductionMethod(); }
    private float freq() { return frequency(); }
    private int saltValue() { return salt(); }
    private Optional<StructurePlacement.ExclusionZone> zone() { return exclusionZone(); }

    private final HolderSet<StructureSet> avoid;
    private final int avoidChunks;

    public ExclusiveSpreadPlacement(Vec3i locateOffset, FrequencyReductionMethod frequencyReductionMethod, float frequency, int salt,
                                    Optional<StructurePlacement.ExclusionZone> exclusionZone, int spacing, int separation, RandomSpreadType spreadType,
                                    HolderSet<StructureSet> avoid, int avoidChunks) {
        super(locateOffset, frequencyReductionMethod, frequency, salt, exclusionZone, spacing, separation, spreadType);
        this.avoid = avoid;
        this.avoidChunks = avoidChunks;
    }

    @Override
    protected boolean isPlacementChunk(@NotNull ChunkGeneratorStructureState state, int x, int z) {
        if (!super.isPlacementChunk(state, x, z)) return false;
        for (var other : avoid) {
            if (state.hasStructureChunkInRange(other, x, z, avoidChunks)) return false;
        }
        return true;
    }

    @Override
    public @NotNull StructurePlacementType<?> type() {
        return ModStructures.EXCLUSIVE_SPREAD.get();
    }
}
