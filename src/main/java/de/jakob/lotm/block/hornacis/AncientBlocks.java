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
 * Blocos das casas antigas da Nação da Noite Eterna (ruínas, Palácio do Pico e mosteiro da Cordilheira de
 * Hornacis). Existem só para as estruturas: não têm receita, não dropam nada e nada os quebra.
 */
public final class AncientBlocks {
    private AncientBlocks() {}

    private static BlockBehaviour.Properties ancient(MapColor color, SoundType sound) {
        return BlockBehaviour.Properties.of().mapColor(color).strength(-1.0f, 3_600_000.0f).noLootTable()
                .sound(sound).pushReaction(PushReaction.BLOCK);
    }

    private static BlockBehaviour.Properties stone() {
        return ancient(MapColor.STONE, SoundType.STONE);
    }

    private static BlockBehaviour.Properties wood() {
        return ancient(MapColor.WOOD, SoundType.WOOD);
    }

    private static BlockBehaviour.Properties slate() {
        return ancient(MapColor.DEEPSLATE, SoundType.DEEPSLATE_TILES);
    }

    private static BlockBehaviour.Properties prop(SoundType sound) {
        return ancient(MapColor.NONE, sound).noOcclusion().isValidSpawn((s, l, p, e) -> false)
                .isRedstoneConductor((s, l, p) -> false).isSuffocating((s, l, p) -> false).isViewBlocking((s, l, p) -> false);
    }

    private static <T extends Block> DeferredBlock<T> reg(String name, Supplier<T> block) {
        DeferredBlock<T> holder = ModBlocks.BLOCKS.register(name, block);
        ModItems.ITEMS.register(name, () -> new BlockItem(holder.get(), new Item.Properties()));
        return holder;
    }

    public static final DeferredBlock<Block> ANCIENT_STONE = reg("ancient_stone", () -> new Block(stone()));
    public static final DeferredBlock<StairBlock> ANCIENT_STONE_STAIRS = reg("ancient_stone_stairs",
            () -> new StairBlock(ANCIENT_STONE.get().defaultBlockState(), stone()));
    public static final DeferredBlock<SlabBlock> ANCIENT_STONE_SLAB = reg("ancient_stone_slab", () -> new SlabBlock(stone()));
    public static final DeferredBlock<WallBlock> ANCIENT_STONE_WALL = reg("ancient_stone_wall", () -> new WallBlock(stone().forceSolidOn()));
    public static final DeferredBlock<Block> CRACKED_ANCIENT_STONE = reg("cracked_ancient_stone", () -> new Block(stone()));
    public static final DeferredBlock<Block> MOSSY_ANCIENT_STONE = reg("mossy_ancient_stone", () -> new Block(stone()));

    /** Reboco de cal das paredes de dentro, e a faixa pintada com o céu da Noite Eterna. */
    public static final DeferredBlock<Block> ANCIENT_PLASTER = reg("ancient_plaster", () -> new Block(ancient(MapColor.SAND, SoundType.CALCITE)));
    public static final DeferredBlock<Block> ANCIENT_FRESCO = reg("ancient_fresco", () -> new Block(ancient(MapColor.SAND, SoundType.CALCITE)));

    public static final DeferredBlock<RotatedPillarBlock> ANCIENT_BEAM = reg("ancient_beam", () -> new RotatedPillarBlock(wood()));
    public static final DeferredBlock<Block> ANCIENT_BOARDS = reg("ancient_boards", () -> new Block(wood()));
    public static final DeferredBlock<StairBlock> ANCIENT_BOARD_STAIRS = reg("ancient_board_stairs",
            () -> new StairBlock(ANCIENT_BOARDS.get().defaultBlockState(), wood()));
    public static final DeferredBlock<SlabBlock> ANCIENT_BOARD_SLAB = reg("ancient_board_slab", () -> new SlabBlock(wood()));

    public static final DeferredBlock<Block> ANCIENT_SLATE = reg("ancient_slate", () -> new Block(slate()));
    public static final DeferredBlock<StairBlock> ANCIENT_SLATE_STAIRS = reg("ancient_slate_stairs",
            () -> new StairBlock(ANCIENT_SLATE.get().defaultBlockState(), slate()));
    public static final DeferredBlock<SlabBlock> ANCIENT_SLATE_SLAB = reg("ancient_slate_slab", () -> new SlabBlock(slate()));

    /** Janela de treliça de madeira (liga com as paredes e outras janelas como uma vidraça). */
    public static final DeferredBlock<IronBarsBlock> ANCIENT_LATTICE = reg("ancient_lattice", () -> new IronBarsBlock(prop(SoundType.WOOD)));

    /** A mesa ainda posta (pratos, talheres, pão, queijo) e as garrafas de vinho pela metade. */
    public static final DeferredBlock<AncientSetTableBlock> ANCIENT_SET_TABLE = reg("ancient_set_table", () -> new AncientSetTableBlock(prop(SoundType.WOOD)));
    public static final DeferredBlock<AncientBottlesBlock> ANCIENT_BOTTLES = reg("ancient_bottles", () -> new AncientBottlesBlock(prop(SoundType.GLASS)));

    /** Castiçal de pedra com a chama amarela que nunca apaga, no chão ou na parede. */
    public static final DeferredBlock<AncientCandlestickBlock> ANCIENT_CANDLESTICK = reg("ancient_candlestick",
            () -> new AncientCandlestickBlock(prop(SoundType.STONE).lightLevel(s -> 14)));

    /** O Selo da Noite Eterna (o Emblema Sagrado das Trevas, em 3 x 3 blocos) e a pedra escura em volta dele. */
    public static final DeferredBlock<EvernightSealBlock> EVERNIGHT_SEAL = reg("evernight_seal",
            () -> new EvernightSealBlock(ancient(MapColor.COLOR_BLACK, SoundType.DEEPSLATE_TILES).lightLevel(s -> 3)));
    public static final DeferredBlock<Block> EVERNIGHT_SEAL_STONE = reg("evernight_seal_stone",
            () -> new Block(ancient(MapColor.COLOR_BLACK, SoundType.DEEPSLATE_TILES)));

    /** Só para o registro acontecer junto com o dos outros blocos do mod. */
    public static void init() {}
}
