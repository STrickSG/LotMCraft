package de.jakob.lotm.structure;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.pools.DimensionPadding;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasLookup;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * A Jigsaw structure that spawns at a Y offset below the surface,
 * but applies terrain adaptation at the surface level (the "adaptation offset").
 *
 * Use this for large structures with deep foundations (e.g. a Victorian city
 * with 8-block dirt/grass basements) so the terrain blends naturally at
 * ground level rather than at the buried spawn point.
 */
public class CityStructure extends Structure {

    public static final MapCodec<CityStructure> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    StructureSettings.CODEC.forGetter(s -> s.modifiableStructureInfo().getOriginalStructureInfo().structureSettings()),
                    StructureTemplatePool.CODEC.fieldOf("start_pool").forGetter(s -> s.startPool),
                    ResourceLocation.CODEC.optionalFieldOf("start_jigsaw_name").forGetter(s -> s.startJigsawName),
                    Codec.intRange(0, 30).fieldOf("size").forGetter(s -> s.size),
                    HeightProvider.CODEC.fieldOf("start_height").forGetter(s -> s.startHeight),
                    Heightmap.Types.CODEC.optionalFieldOf("project_start_to_heightmap").forGetter(s -> s.projectStartToHeightmap),
                    Codec.intRange(1, 128).fieldOf("max_distance_from_center").forGetter(s -> s.maxDistanceFromCenter),
                    // How many blocks below the surface the structure actually spawns.
                    // E.g. 8 means the structure origin is 8 blocks underground, but
                    // terrain adaptation will still be calculated at surface level.
                    Codec.intRange(0, 64).fieldOf("foundation_depth").forGetter(s -> s.foundationDepth),
                    DimensionPadding.CODEC.optionalFieldOf("dimension_padding", JigsawStructure.DEFAULT_DIMENSION_PADDING).forGetter(s -> s.dimensionPadding),
                    LiquidSettings.CODEC.optionalFieldOf("liquid_settings", JigsawStructure.DEFAULT_LIQUID_SETTINGS).forGetter(s -> s.liquidSettings),
                    TerrainAdjustment.CODEC.optionalFieldOf("terrain_adaptation", TerrainAdjustment.NONE).forGetter(s -> s.terrainAdaptation)
            ).apply(instance, CityStructure::new));

    private final Holder<StructureTemplatePool> startPool;
    private final Optional<ResourceLocation> startJigsawName;
    private final int size;
    private final HeightProvider startHeight;
    private final Optional<Heightmap.Types> projectStartToHeightmap;
    private final int maxDistanceFromCenter;
    private final int foundationDepth;
    private final DimensionPadding dimensionPadding;
    private final LiquidSettings liquidSettings;
    private final TerrainAdjustment terrainAdaptation;

    public CityStructure(StructureSettings config,
                         Holder<StructureTemplatePool> startPool,
                         Optional<ResourceLocation> startJigsawName,
                         int size,
                         HeightProvider startHeight,
                         Optional<Heightmap.Types> projectStartToHeightmap,
                         int maxDistanceFromCenter,
                         int foundationDepth,
                         DimensionPadding dimensionPadding,
                         LiquidSettings liquidSettings,
                         TerrainAdjustment terrainAdaptation) {
        super(config);
        this.startPool = startPool;
        this.startJigsawName = startJigsawName;
        this.size = size;
        this.startHeight = startHeight;
        this.projectStartToHeightmap = projectStartToHeightmap;
        this.maxDistanceFromCenter = maxDistanceFromCenter;
        this.foundationDepth = foundationDepth;
        this.dimensionPadding = dimensionPadding;
        this.liquidSettings = liquidSettings;
        this.terrainAdaptation = terrainAdaptation;
    }

    @Override
    public @NotNull TerrainAdjustment terrainAdaptation() {
        return this.terrainAdaptation;
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunkPos = context.chunkPos();
        ChunkGenerator gen = context.chunkGenerator();

        int surfaceY = this.startHeight.sample(
                context.random(),
                new WorldGenerationContext(gen, context.heightAccessor()));

        if (this.projectStartToHeightmap.isPresent()) {
            surfaceY += gen.getFirstOccupiedHeight(
                    chunkPos.getMinBlockX(), chunkPos.getMinBlockZ(),
                    this.projectStartToHeightmap.get(),
                    context.heightAccessor(), context.randomState());
        }

        BlockPos placementPos = new BlockPos(chunkPos.getMinBlockX(), surfaceY - this.foundationDepth, chunkPos.getMinBlockZ());

        Optional<GenerationStub> stub = JigsawPlacement.addPieces(
                context, this.startPool, this.startJigsawName, this.size,
                placementPos, false, Optional.empty(),
                this.maxDistanceFromCenter, PoolAliasLookup.EMPTY,
                this.dimensionPadding, this.liquidSettings);
        if (stub.isEmpty()) return stub;

        // A cidade (113 x 101 blocos) começa no canto do chunk e gira: a checagem antiga olhava só em volta do meio
        // do chunk e deixava a cidade nascer sobre rios e lagos. Agora as peças são montadas uma vez e a área inteira
        // que elas ocupam é conferida: nada de água, e o terreno não pode variar demais (senão partes ficam no ar).
        StructurePiecesBuilder pieces = stub.get().getPiecesBuilder();
        BoundingBox box = pieces.getBoundingBox();
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        for (int i = 0; i <= 4; i++) {
            for (int j = 0; j <= 4; j++) {
                int x = box.minX() + (box.getXSpan() - 1) * i / 4;
                int z = box.minZ() + (box.getZSpan() - 1) * j / 4;
                int floor = gen.getFirstOccupiedHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState());
                NoiseColumn column = gen.getBaseColumn(x, z, context.heightAccessor(), context.randomState());
                if (!column.getBlock(floor).getFluidState().isEmpty()) return Optional.empty();
                min = Math.min(min, floor);
                max = Math.max(max, floor);
            }
        }
        if (max - min > MAX_HEIGHT_DIFFERENCE) return Optional.empty();
        // as peças já montadas vão junto (montar de novo gastaria o sorteio de novo)
        return Optional.of(new GenerationStub(stub.get().position(), Either.right(pieces)));
    }

    private static final int MAX_HEIGHT_DIFFERENCE = 8;

    @Override
    public StructureType<?> type() {
        return ModStructures.CITY_STRUCTURE.get();
    }
}