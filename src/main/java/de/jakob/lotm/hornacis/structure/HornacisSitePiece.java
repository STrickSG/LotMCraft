package de.jakob.lotm.hornacis.structure;

import de.jakob.lotm.structure.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
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
        // as entidades são postas à parte (placeEntities)
        return new StructurePlaceSettings().setRotation(rotation).setRotationPivot(pivot).setIgnoreEntities(true)
                .addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK)
                .setLiquidSettings(LiquidSettings.IGNORE_WATERLOGGING);
    }

    @Override
    public void postProcess(@NotNull WorldGenLevel level, @NotNull StructureManager structures, @NotNull ChunkGenerator generator,
                            @NotNull RandomSource random, @NotNull BoundingBox box, @NotNull ChunkPos chunk, @NotNull BlockPos origin) {
        super.postProcess(level, structures, generator, random, box, chunk, origin);
        placeEntities(level, box);
    }

    /**
     * Como no StructureTemplate do vanilla, mas também acerta o bloco onde molduras e pinturas ficam presas
     * (TileX/Y/Z): no template ele é relativo e, sem isso, a entidade reclama de "posição inválida".
     */
    private void placeEntities(ServerLevelAccessor level, BoundingBox box) {
        Mirror mirror = placeSettings.getMirror();
        for (StructureTemplate.StructureEntityInfo info : template.entityInfoList) {
            BlockPos block = StructureTemplate.transform(info.blockPos, mirror, rotation, pivot).offset(templatePosition);
            if (!box.isInside(block)) continue;
            Vec3 pos = StructureTemplate.transform(info.pos, mirror, rotation, pivot).add(templatePosition.getX(), templatePosition.getY(), templatePosition.getZ());
            CompoundTag tag = info.nbt.copy();
            ListTag list = new ListTag();
            list.add(DoubleTag.valueOf(pos.x));
            list.add(DoubleTag.valueOf(pos.y));
            list.add(DoubleTag.valueOf(pos.z));
            tag.put("Pos", list);
            tag.remove("UUID");
            if (tag.contains("TileX")) {
                tag.putInt("TileX", block.getX());
                tag.putInt("TileY", block.getY());
                tag.putInt("TileZ", block.getZ());
            }
            EntityType.create(tag, level.getLevel()).ifPresent(entity -> {
                float yaw = entity.rotate(rotation) + entity.mirror(mirror) - entity.getYRot();
                entity.moveTo(pos.x, pos.y, pos.z, yaw, entity.getXRot());
                level.addFreshEntityWithPassengers(entity);
            });
        }
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
