package de.jakob.lotm.hornacis.structure;

import de.jakob.lotm.structure.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.neoforged.neoforge.common.world.PieceBeardifierModifier;
import org.jetbrains.annotations.NotNull;

/**
 * Uma estrutura de Hornacis num terraço: o centro do template fica no centro do lugar, girado em volta do próprio
 * centro. A camada y=0 do template é a fundação, y=1 o chão (o bloco de cima do terreno) e y=2 o primeiro bloco de
 * ar; o terreno em volta é ajustado para encontrar o chão (beard_thin com o "nível do chão" 2 acima da base).
 */
public class HornacisSitePiece extends TemplateStructurePiece implements PieceBeardifierModifier {
    /** Do fundo do template até o primeiro bloco de ar acima do chão */
    public static final int GROUND = 2;

    private final Rotation rotation;
    private final BlockPos pivot;

    public HornacisSitePiece(StructureTemplateManager manager, ResourceLocation template, int x, int y, int z, Rotation rotation) {
        this(manager, template, rotation, centerOf(manager, template), x, y, z);
    }

    private HornacisSitePiece(StructureTemplateManager manager, ResourceLocation template, Rotation rotation, BlockPos pivot, int x, int y, int z) {
        super(ModStructures.HORNACIS_SITE_PIECE.get(), 0, manager, template, template.toString(), settings(rotation, pivot),
                new BlockPos(x - pivot.getX(), y - GROUND, z - pivot.getZ()));
        this.rotation = rotation;
        this.pivot = pivot;
    }

    public HornacisSitePiece(StructureTemplateManager manager, CompoundTag tag) {
        super(ModStructures.HORNACIS_SITE_PIECE.get(), tag, manager,
                id -> settings(Rotation.valueOf(tag.getString("Rot")), new BlockPos(tag.getInt("PX"), 0, tag.getInt("PZ"))));
        this.rotation = Rotation.valueOf(tag.getString("Rot"));
        this.pivot = new BlockPos(tag.getInt("PX"), 0, tag.getInt("PZ"));
    }

    private static BlockPos centerOf(StructureTemplateManager manager, ResourceLocation template) {
        Vec3i size = manager.getOrCreate(template).getSize();
        return new BlockPos(size.getX() / 2, 0, size.getZ() / 2);
    }

    private static StructurePlaceSettings settings(Rotation rotation, BlockPos pivot) {
        return new StructurePlaceSettings().setRotation(rotation).setRotationPivot(pivot)
                .addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK)
                .setLiquidSettings(LiquidSettings.IGNORE_WATERLOGGING);
    }

    @Override
    protected void addAdditionalSaveData(@NotNull StructurePieceSerializationContext context, @NotNull CompoundTag tag) {
        super.addAdditionalSaveData(context, tag);
        tag.putString("Rot", rotation.name());
        tag.putInt("PX", pivot.getX());
        tag.putInt("PZ", pivot.getZ());
    }

    @Override
    protected void handleDataMarker(@NotNull String name, @NotNull BlockPos pos, @NotNull ServerLevelAccessor level, @NotNull RandomSource random, @NotNull BoundingBox box) {
    }

    @Override
    public BoundingBox getBeardifierBox() {
        return boundingBox;
    }

    @Override
    public TerrainAdjustment getTerrainAdjustment() {
        return TerrainAdjustment.BEARD_THIN;
    }

    @Override
    public int getGroundLevelDelta() {
        return GROUND;
    }
}
