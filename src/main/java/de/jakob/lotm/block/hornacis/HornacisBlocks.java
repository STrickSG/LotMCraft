package de.jakob.lotm.block.hornacis;

import de.jakob.lotm.block.ModBlocks;
import de.jakob.lotm.item.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;

import java.util.function.Supplier;

/**
 * Blocos da arquitetura da Nação da Noite Eterna, usados nas estruturas da Cordilheira de Hornacis (ruínas,
 * palácio do pico, mosteiro): pedra da montanha, colunas, a lua crescente e as estrelas da Noite Eterna, vitrais,
 * telhas das cúpulas, os castiçais de pedra de chama amarela e o Selo da Noite Eterna (indestrutível).
 */
public final class HornacisBlocks {
    private HornacisBlocks() {}

    private static BlockBehaviour.Properties stone() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.STONE).requiresCorrectToolForDrops().strength(1.8f, 6.0f).sound(SoundType.STONE);
    }

    private static BlockBehaviour.Properties dark() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLUE).requiresCorrectToolForDrops().strength(2.5f, 6.0f).sound(SoundType.DEEPSLATE_BRICKS);
    }

    private static <T extends Block> DeferredBlock<T> reg(String name, Supplier<T> block) {
        DeferredBlock<T> holder = ModBlocks.BLOCKS.register(name, block);
        ModItems.ITEMS.register(name, () -> new BlockItem(holder.get(), new Item.Properties()));
        return holder;
    }

    public static final DeferredBlock<Block> HORNACIS_STONE_BRICKS = reg("hornacis_stone_bricks", () -> new Block(stone()));
    public static final DeferredBlock<StairBlock> HORNACIS_STONE_BRICK_STAIRS = reg("hornacis_stone_brick_stairs",
            () -> new StairBlock(HORNACIS_STONE_BRICKS.get().defaultBlockState(), stone()));
    public static final DeferredBlock<SlabBlock> HORNACIS_STONE_BRICK_SLAB = reg("hornacis_stone_brick_slab", () -> new SlabBlock(stone()));
    public static final DeferredBlock<WallBlock> HORNACIS_STONE_BRICK_WALL = reg("hornacis_stone_brick_wall", () -> new WallBlock(stone().forceSolidOn()));
    public static final DeferredBlock<Block> CRACKED_HORNACIS_STONE_BRICKS = reg("cracked_hornacis_stone_bricks", () -> new Block(stone()));
    public static final DeferredBlock<Block> MOSSY_HORNACIS_STONE_BRICKS = reg("mossy_hornacis_stone_bricks", () -> new Block(stone()));
    public static final DeferredBlock<Block> POLISHED_HORNACIS_STONE = reg("polished_hornacis_stone", () -> new Block(stone()));
    public static final DeferredBlock<StairBlock> POLISHED_HORNACIS_STONE_STAIRS = reg("polished_hornacis_stone_stairs",
            () -> new StairBlock(POLISHED_HORNACIS_STONE.get().defaultBlockState(), stone()));
    public static final DeferredBlock<SlabBlock> POLISHED_HORNACIS_STONE_SLAB = reg("polished_hornacis_stone_slab", () -> new SlabBlock(stone()));
    public static final DeferredBlock<RotatedPillarBlock> HORNACIS_COLUMN = reg("hornacis_column", () -> new RotatedPillarBlock(stone()));
    public static final DeferredBlock<Block> HORNACIS_COLUMN_CAPITAL = reg("hornacis_column_capital", () -> new Block(stone()));

    public static final DeferredBlock<Block> EVERNIGHT_CARVED_STONE = reg("evernight_carved_stone", () -> new Block(dark()));
    public static final DeferredBlock<Block> EVERNIGHT_TILES = reg("evernight_tiles", () -> new Block(dark()));
    public static final DeferredBlock<SlabBlock> EVERNIGHT_TILE_SLAB = reg("evernight_tile_slab", () -> new SlabBlock(dark()));
    public static final DeferredBlock<TransparentBlock> EVERNIGHT_GLASS = reg("evernight_glass", () -> new TransparentBlock(
            BlockBehaviour.Properties.of().strength(0.3f).sound(SoundType.GLASS).noOcclusion().isValidSpawn((s, l, p, e) -> false)
                    .isRedstoneConductor((s, l, p) -> false).isSuffocating((s, l, p) -> false).isViewBlocking((s, l, p) -> false)));
    public static final DeferredBlock<IronBarsBlock> EVERNIGHT_GLASS_PANE = reg("evernight_glass_pane", () -> new IronBarsBlock(
            BlockBehaviour.Properties.of().strength(0.3f).sound(SoundType.GLASS).noOcclusion()));

    public static final DeferredBlock<Block> DOME_TILES = reg("dome_tiles", () -> new Block(stone().mapColor(MapColor.COLOR_LIGHT_BLUE).sound(SoundType.COPPER)));
    public static final DeferredBlock<StairBlock> DOME_TILE_STAIRS = reg("dome_tile_stairs",
            () -> new StairBlock(DOME_TILES.get().defaultBlockState(), stone().mapColor(MapColor.COLOR_LIGHT_BLUE).sound(SoundType.COPPER)));
    public static final DeferredBlock<SlabBlock> DOME_TILE_SLAB = reg("dome_tile_slab", () -> new SlabBlock(stone().mapColor(MapColor.COLOR_LIGHT_BLUE).sound(SoundType.COPPER)));

    /** Castiçal de pedra com a chama amarela que nunca apaga (como os embutidos nas paredes do palácio). */
    public static final DeferredBlock<StoneCandlestickBlock> STONE_CANDLESTICK = reg("stone_candlestick", () -> new StoneCandlestickBlock(
            stone().noOcclusion().lightLevel(s -> 14).pushReaction(PushReaction.DESTROY)));

    /** O Selo da Noite Eterna (o Emblema Sagrado das Trevas) e a pedra do selo: nada os quebra. */
    public static final DeferredBlock<Block> EVERNIGHT_SEAL = reg("evernight_seal", () -> new Block(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_BLACK).strength(-1.0f, 3_600_000.0f).noLootTable().lightLevel(s -> 5)
            .sound(SoundType.DEEPSLATE_TILES).pushReaction(PushReaction.BLOCK).isValidSpawn((s, l, p, e) -> false)));
    public static final DeferredBlock<Block> EVERNIGHT_SEAL_STONE = reg("evernight_seal_stone", () -> new Block(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_BLACK).strength(-1.0f, 3_600_000.0f).noLootTable()
            .sound(SoundType.DEEPSLATE_TILES).pushReaction(PushReaction.BLOCK).isValidSpawn((s, l, p, e) -> false)));

    /** Só para o registro acontecer junto com o dos outros blocos do mod. */
    public static void init() {}
}
