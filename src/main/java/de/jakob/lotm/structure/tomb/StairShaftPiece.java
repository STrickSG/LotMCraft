package de.jakob.lotm.structure.tomb;

import de.jakob.lotm.structure.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.neoforged.neoforge.common.world.PieceBeardifierModifier;
import org.jetbrains.annotations.NotNull;

/**
 * Escadaria em espiral que liga a tumba da superfície às ruínas lá embaixo.
 * O tamanho depende da altura do terreno, por isso é feita em código (5x5: paredes, degraus e coluna central).
 */
public class StairShaftPiece extends StructurePiece implements PieceBeardifierModifier {
    // as 8 casas em volta da coluna central, em sentido horário; a escada sobe uma casa por bloco
    private static final int[][] RING = {{-1, -1}, {0, -1}, {1, -1}, {1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}};
    private final int cx, cz, bottom, top, openRows;

    /** openRows: quantas camadas de baixo ficam sem parede (a espiral desce aberta dentro da sala das ruínas). */
    public StairShaftPiece(int cx, int cz, int bottom, int top, int openRows) {
        super(ModStructures.STAIR_SHAFT_PIECE.get(), 0, new BoundingBox(cx - 2, bottom, cz - 2, cx + 2, top, cz + 2));
        this.cx = cx; this.cz = cz; this.bottom = bottom; this.top = top; this.openRows = openRows;
    }

    public StairShaftPiece(CompoundTag tag) {
        super(ModStructures.STAIR_SHAFT_PIECE.get(), tag);
        this.cx = tag.getInt("CX"); this.cz = tag.getInt("CZ"); this.bottom = tag.getInt("Bottom"); this.top = tag.getInt("Top");
        this.openRows = tag.getInt("Open");
    }

    @Override
    protected void addAdditionalSaveData(@NotNull StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt("CX", cx); tag.putInt("CZ", cz); tag.putInt("Bottom", bottom); tag.putInt("Top", top); tag.putInt("Open", openRows);
    }

    @Override
    public void postProcess(@NotNull WorldGenLevel level, @NotNull StructureManager structureManager, @NotNull ChunkGenerator generator,
                            @NotNull RandomSource random, @NotNull BoundingBox chunkBox, @NotNull ChunkPos chunkPos, @NotNull BlockPos pivot) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = bottom; y <= top; y++) {
            int step = Math.floorMod(y - bottom, RING.length);
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    pos.set(cx + dx, y, cz + dz);
                    if (!chunkBox.isInside(pos)) continue;
                    BlockState state;
                    if (Math.max(Math.abs(dx), Math.abs(dz)) == 2) {
                        if (y < bottom + openRows) continue;
                        // parede; uma lanterna a cada 7 blocos
                        state = (dx == 2 && dz == 0 && Math.floorMod(y - bottom, 7) == 3) ? Blocks.LANTERN.defaultBlockState() : wall(random, y);
                    } else if (dx == 0 && dz == 0) {
                        state = y < 0 ? Blocks.POLISHED_DEEPSLATE.defaultBlockState() : Blocks.STONE_BRICKS.defaultBlockState();
                    } else if (RING[step][0] == dx && RING[step][1] == dz) {
                        int[] next = RING[(step + 1) % RING.length];
                        Direction facing = Direction.fromDelta(next[0] - dx, 0, next[1] - dz);
                        state = (y < 0 ? Blocks.DEEPSLATE_BRICK_STAIRS : Blocks.STONE_BRICK_STAIRS).defaultBlockState()
                                .setValue(StairBlock.FACING, facing == null ? Direction.NORTH : facing);
                    } else {
                        state = Blocks.AIR.defaultBlockState();
                    }
                    level.setBlock(pos, state, 2);
                }
            }
        }
    }

    private static BlockState wall(RandomSource random, int y) {
        if (y < 0) {
            int r = random.nextInt(10);
            return (r < 6 ? Blocks.DEEPSLATE_BRICKS : r < 9 ? Blocks.CRACKED_DEEPSLATE_BRICKS : Blocks.DEEPSLATE_TILES).defaultBlockState();
        }
        int r = random.nextInt(10);
        return (r < 5 ? Blocks.STONE_BRICKS : r < 8 ? Blocks.MOSSY_STONE_BRICKS : Blocks.CRACKED_STONE_BRICKS).defaultBlockState();
    }

    @Override
    public BoundingBox getBeardifierBox() { return boundingBox; }

    @Override
    public TerrainAdjustment getTerrainAdjustment() { return TerrainAdjustment.NONE; }

    @Override
    public int getGroundLevelDelta() { return 0; }
}
