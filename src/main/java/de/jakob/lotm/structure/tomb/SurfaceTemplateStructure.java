package de.jakob.lotm.structure.tomb;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.jakob.lotm.structure.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Um modelo só, na superfície (ex.: o Observatório). O "anchor" é o ponto do modelo que fica no nível do chão.
 * Não exige terreno plano: o terreno em volta se ajusta ao modelo (terrain_adjustment, padrão beard_box).
 */
public class SurfaceTemplateStructure extends Structure {
    public static final MapCodec<SurfaceTemplateStructure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            settingsCodec(instance),
            ResourceLocation.CODEC.fieldOf("template").forGetter(s -> s.template),
            BlockPos.CODEC.fieldOf("anchor").forGetter(s -> s.anchor),
            TerrainAdjustment.CODEC.optionalFieldOf("terrain_adjustment", TerrainAdjustment.BEARD_BOX).forGetter(s -> s.adjustment),
            Codec.BOOL.optionalFieldOf("ignore_biome", true).forGetter(s -> s.ignoreBiome),
            Codec.intRange(0, 10_000).optionalFieldOf("max_per_world", 0).forGetter(s -> s.maxPerWorld)
    ).apply(instance, SurfaceTemplateStructure::new));

    private final ResourceLocation template;
    private final BlockPos anchor;
    private final TerrainAdjustment adjustment;
    /** O bioma já foi escolhido pelo posicionamento (far_ring): não confere de novo aqui */
    private final boolean ignoreBiome;
    /** Quantas desta estrutura podem existir no mundo (0 = sem limite) */
    private final int maxPerWorld;

    public SurfaceTemplateStructure(StructureSettings settings, ResourceLocation template, BlockPos anchor, TerrainAdjustment adjustment,
                                    boolean ignoreBiome, int maxPerWorld) {
        super(settings);
        this.template = template; this.anchor = anchor; this.adjustment = adjustment;
        this.ignoreBiome = ignoreBiome; this.maxPerWorld = maxPerWorld;
    }

    @Override
    protected @NotNull Optional<GenerationStub> findGenerationPoint(@NotNull GenerationContext context) {
        ChunkPos chunk = context.chunkPos();
        // são poucas por mundo: procura um ponto seco e do bioma certo no chunk antes de desistir (não nasce na água)
        int x = 0, z = 0, ground = 0;
        boolean found = false;
        for (int i = 0; i < 9 && !found; i++) {
            x = chunk.getBlockX(8 + (i % 3 == 1 ? -6 : i % 3 == 2 ? 6 : 0));
            z = chunk.getBlockZ(8 + (i / 3 == 1 ? -6 : i / 3 == 2 ? 6 : 0));
            ground = context.chunkGenerator().getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
            NoiseColumn column = context.chunkGenerator().getBaseColumn(x, z, context.heightAccessor(), context.randomState());
            found = column.getBlock(ground).getFluidState().isEmpty() && column.getBlock(ground + 1).isAir()
                    // o bioma é conferido no ponto escolhido: em volta pode ter rio ou outro bioma
                    && context.validBiome().test(context.chunkGenerator().getBiomeSource().getNoiseBiome(
                            QuartPos.fromBlock(x), QuartPos.fromBlock(ground), QuartPos.fromBlock(z), context.randomState().sampler()));
        }
        if (!found) {
            // nenhum ponto seco e do bioma certo: aceita o meio do chunk (são poucas por mundo, não podem faltar)
            x = chunk.getMiddleBlockX(); z = chunk.getMiddleBlockZ();
            ground = context.chunkGenerator().getFirstOccupiedHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState());
        }

        Rotation rotation = Rotation.getRandom(context.random());
        BlockPos rel = StructureTemplate.calculateRelativePosition(TombTemplatePiece.settings(rotation), anchor);
        BlockPos pos = new BlockPos(x - rel.getX(), ground + 1 - anchor.getY(), z - rel.getZ());
        return Optional.of(new GenerationStub(new BlockPos(x, ground, z), builder ->
                builder.addPiece(new TombTemplatePiece(context.structureTemplateManager(), template, pos, rotation, adjustment))));
    }

    @Override
    public @NotNull Optional<GenerationStub> findValidGenerationPoint(@NotNull GenerationContext context) {
        if (maxPerWorld > 0 && !de.jakob.lotm.structure.StructureCap.hasRoom(template, maxPerWorld)) return Optional.empty();
        // far_ring: o bioma já foi escolhido pelo posicionamento, aqui não pode faltar estrutura por causa dele
        return ignoreBiome ? findGenerationPoint(context) : super.findValidGenerationPoint(context);
    }

    /** Com limite por mundo: só cria se ainda houver lugar (a reserva é feita aqui, quando a estrutura nasce de verdade) */
    @Override
    public @NotNull net.minecraft.world.level.levelgen.structure.StructureStart generate(net.minecraft.core.RegistryAccess registryAccess,
            net.minecraft.world.level.chunk.ChunkGenerator chunkGenerator, net.minecraft.world.level.biome.BiomeSource biomeSource,
            net.minecraft.world.level.levelgen.RandomState randomState,
            net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager structureTemplateManager, long seed,
            ChunkPos chunkPos, int references, net.minecraft.world.level.LevelHeightAccessor heightAccessor,
            java.util.function.Predicate<net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome>> validBiome) {
        var start = super.generate(registryAccess, chunkGenerator, biomeSource, randomState, structureTemplateManager, seed, chunkPos, references, heightAccessor, validBiome);
        if (maxPerWorld > 0 && start.isValid() && !de.jakob.lotm.structure.StructureCap.claim(template, maxPerWorld, chunkPos)) {
            return net.minecraft.world.level.levelgen.structure.StructureStart.INVALID_START;
        }
        return start;
    }

    /** Onde já nasceram (só para estruturas com limite por mundo) */
    public java.util.List<ChunkPos> created() {
        return de.jakob.lotm.structure.StructureCap.created(template);
    }

    public int maxPerWorld() {
        return maxPerWorld;
    }

    @Override
    public @NotNull StructureType<?> type() {
        return ModStructures.SURFACE_TEMPLATE.get();
    }
}
