package de.jakob.lotm.structure.tomb;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.jakob.lotm.structure.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Estrutura subterrânea com entrada na superfície (como as Cidades Antigas, mas com uma tumba em cima):
 * a tumba fica no chão, as ruínas ficam "depth" blocos abaixo e uma escadaria em espiral liga as duas.
 * Os "anchor" são os pontos de cada modelo onde a escadaria encosta (coordenadas dentro do .nbt).
 */
public class TombRuinsStructure extends Structure {
    public static final MapCodec<TombRuinsStructure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            settingsCodec(instance),
            ResourceLocation.CODEC.fieldOf("tomb").forGetter(s -> s.tomb),
            BlockPos.CODEC.fieldOf("tomb_anchor").forGetter(s -> s.tombAnchor),
            ResourceLocation.CODEC.fieldOf("ruins").forGetter(s -> s.ruins),
            BlockPos.CODEC.fieldOf("ruins_anchor").forGetter(s -> s.ruinsAnchor),
            IntProvider.codec(10, 200).fieldOf("depth").forGetter(s -> s.depth)
    ).apply(instance, TombRuinsStructure::new));

    /** a antessala das ruínas tem 7 de altura: a espiral desce aberta por ela */
    private static final int OPEN_ROWS = 7;
    private final ResourceLocation tomb, ruins;
    private final BlockPos tombAnchor, ruinsAnchor;
    private final IntProvider depth;

    public TombRuinsStructure(StructureSettings settings, ResourceLocation tomb, BlockPos tombAnchor, ResourceLocation ruins, BlockPos ruinsAnchor, IntProvider depth) {
        super(settings);
        this.tomb = tomb; this.tombAnchor = tombAnchor; this.ruins = ruins; this.ruinsAnchor = ruinsAnchor; this.depth = depth;
    }

    @Override
    protected @NotNull Optional<GenerationStub> findGenerationPoint(@NotNull GenerationContext context) {
        ChunkPos chunk = context.chunkPos();
        // só existem 3 por mundo: procura um ponto seco e do bioma certo no chunk antes de desistir
        // (a tumba não nasce na água nem flutuando)
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
        if (!found) return Optional.empty();

        int minY = context.heightAccessor().getMinBuildHeight() + 6;
        int ruinsFloor = Math.max(minY + ruinsAnchor.getY(), ground - depth.sample(context.random()));
        if (ground - ruinsFloor < 12) return Optional.empty();

        Rotation rotation = Rotation.getRandom(context.random());
        StructurePlaceSettings settings = TombTemplatePiece.settings(rotation);
        BlockPos tombRel = StructureTemplate.calculateRelativePosition(settings, tombAnchor);
        BlockPos ruinsRel = StructureTemplate.calculateRelativePosition(settings, ruinsAnchor);
        BlockPos tombPos = new BlockPos(x - tombRel.getX(), ground - tombAnchor.getY(), z - tombRel.getZ());
        BlockPos ruinsPos = new BlockPos(x - ruinsRel.getX(), ruinsFloor - ruinsAnchor.getY(), z - ruinsRel.getZ());

        int fx = x, fz = z, fGround = ground;
        return Optional.of(new GenerationStub(new BlockPos(x, ground, z), builder -> {
            var manager = context.structureTemplateManager();
            builder.addPiece(new TombTemplatePiece(manager, ruins, ruinsPos, rotation, TerrainAdjustment.ENCAPSULATE));
            builder.addPiece(new StairShaftPiece(fx, fz, ruinsFloor, fGround - 1, OPEN_ROWS));
            builder.addPiece(new TombTemplatePiece(manager, tomb, tombPos, rotation, TerrainAdjustment.BEARD_THIN));
        }));
    }

    @Override
    public @NotNull StructureType<?> type() {
        return ModStructures.TOMB_RUINS.get();
    }
}
