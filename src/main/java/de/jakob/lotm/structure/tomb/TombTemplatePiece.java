package de.jakob.lotm.structure.tomb;

import de.jakob.lotm.structure.ModStructures;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.neoforged.neoforge.common.world.PieceBeardifierModifier;
import org.jetbrains.annotations.NotNull;

/** Uma parte da estrutura (tumba ou ruínas) com o seu próprio ajuste de terreno. */
public class TombTemplatePiece extends TemplateStructurePiece implements PieceBeardifierModifier {
    private final Rotation rotation;
    private final TerrainAdjustment adjustment;

    public TombTemplatePiece(StructureTemplateManager manager, ResourceLocation template, BlockPos pos, Rotation rotation, TerrainAdjustment adjustment) {
        super(ModStructures.TOMB_TEMPLATE_PIECE.get(), 0, manager, template, template.toString(), settings(rotation), pos);
        this.rotation = rotation;
        this.adjustment = adjustment;
    }

    public TombTemplatePiece(StructureTemplateManager manager, CompoundTag tag) {
        super(ModStructures.TOMB_TEMPLATE_PIECE.get(), tag, manager, id -> settings(Rotation.valueOf(tag.getString("Rot"))));
        this.rotation = Rotation.valueOf(tag.getString("Rot"));
        this.adjustment = TerrainAdjustment.valueOf(tag.getString("Adjust"));
    }

    public static StructurePlaceSettings settings(Rotation rotation) {
        return new StructurePlaceSettings().setRotation(rotation).addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK);
    }

    @Override
    protected void addAdditionalSaveData(@NotNull StructurePieceSerializationContext context, @NotNull CompoundTag tag) {
        super.addAdditionalSaveData(context, tag);
        tag.putString("Rot", rotation.name());
        tag.putString("Adjust", adjustment.name());
    }

    public Rotation getRotation() { return rotation; }
    public BlockPos getTemplatePosition() { return templatePosition; }
    public String getTemplateName() { return templateName; }

    @Override
    protected void handleDataMarker(@NotNull String name, @NotNull BlockPos pos, @NotNull ServerLevelAccessor level, @NotNull RandomSource random, @NotNull BoundingBox box) {
    }

    @Override
    public BoundingBox getBeardifierBox() { return boundingBox; }

    @Override
    public TerrainAdjustment getTerrainAdjustment() { return adjustment; }

    @Override
    public int getGroundLevelDelta() { return 0; }
}
