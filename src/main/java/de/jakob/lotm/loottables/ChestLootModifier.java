package de.jakob.lotm.loottables;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.jakob.lotm.LOTMCraft;
import de.jakob.lotm.beyonders.rituals.RitualManager;
import de.jakob.lotm.gui.custom.ritualistic_table.RitualMenu;
import de.jakob.lotm.item.ModIngredients;
import de.jakob.lotm.beyonders.potions.BeyonderCharacteristicItemHandler;
import de.jakob.lotm.beyonders.potions.PotionItemHandler;
import de.jakob.lotm.beyonders.potions.PotionRecipeItemHandler;
import de.jakob.lotm.beyonders.potions.BlasphemyCardItemHandler;
import de.jakob.lotm.util.BeyonderData;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

public class ChestLootModifier extends LootModifier {

    public static final Supplier<MapCodec<ChestLootModifier>> CODEC = () ->
            RecordCodecBuilder.mapCodec(inst -> codecStart(inst).apply(inst, ChestLootModifier::new));

    public ChestLootModifier(LootItemCondition[] conditionsIn) {
        super(conditionsIn);
    }

    private static final Random random = new Random();

    // Barris da grande Igreja da Noite Eterna e da Igreja do Sol
    private static final ResourceLocation EVERNIGHT_CHURCH = ResourceLocation.fromNamespaceAndPath(LOTMCraft.MOD_ID, "chests/evernight_church");
    private static final ResourceLocation SUN_CHURCH = ResourceLocation.fromNamespaceAndPath(LOTMCraft.MOD_ID, "chests/blazing_sun_church");
    // A Igreja da Noite Eterna guarda os Caminhos do Insone (Escuridão) e da Morte, que ainda não tem estrutura própria
    private static final List<String> EVERNIGHT_PATHWAYS = List.of("darkness", "death");
    private static final List<String> SUN_PATHWAYS = List.of("sun");

    /** Caminhos permitidos para os itens Beyonder deste baú, ou null quando pode ser qualquer um. */
    public static List<String> allowedPathways(ResourceLocation lootTable) {
        if (EVERNIGHT_CHURCH.equals(lootTable)) return EVERNIGHT_PATHWAYS;
        if (SUN_CHURCH.equals(lootTable)) return SUN_PATHWAYS;
        return null;
    }

    /** Caminho dos itens Beyonder sorteados para este baú. */
    public static String pathwayFor(ResourceLocation lootTable) {
        List<String> allowed = allowedPathways(lootTable);
        List<String> pathways = allowed != null ? allowed : BeyonderData.implementedPathways;
        return pathways.get(random.nextInt(pathways.size()));
    }

    @Override
    protected @NotNull ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        ResourceLocation lootTable = context.getQueriedLootTableId();
        if (lootTable.getPath().contains("chests/")) {
            boolean anyPathway = allowedPathways(lootTable) == null;
            if (context.getRandom().nextFloat() < 0.45f) {
                String pathway = pathwayFor(lootTable);
                int sequence = getWeightedHighSequence();
                ItemStack item = getRandomLoot(pathway, sequence, anyPathway);

                if (item != null && (sequence >= 5)) {
                    generatedLoot.add(item);
                }
            }
            // Carta da Blasfêmia: muito rara (1% dos baús), pois vale como receita do Caminho inteiro
            if (context.getRandom().nextFloat() < 0.01f) {
                ItemStack card = anyPathway ? BlasphemyCardItemHandler.randomCard(random) : BlasphemyCardItemHandler.cardOf(pathwayFor(lootTable));
                if (!card.isEmpty()) generatedLoot.add(card);
            }
        }

        return generatedLoot;
    }

    public static int getWeightedHighSequence() {
        Random random = new Random();
        double normalizedValue = random.nextDouble(); // 0.0 to 1.0

        double weighted = Math.pow(normalizedValue, 0.35); // Lower exponent = stronger bias toward high values

        // Map to range 1-9 (weighted now favors values close to 1.0, which maps to 9)
        return 1 + (int) (weighted * 9);
    }

    public static ItemStack getRandomLoot(String pathway, int sequence) {
        return getRandomLoot(pathway, sequence, true);
    }

    // Os livros de ritual não são de nenhum Caminho: ficam de fora onde só um Caminho pode aparecer
    public static ItemStack getRandomLoot(String pathway, int sequence, boolean ritualBooks) {
        return switch(random.nextInt(ritualBooks ? 8 : 7)) {
            case 1,2 -> {
                Item item = ModIngredients.selectRandomIngredientOfPathwayAndSequence(random, pathway, sequence);
                yield item != null ? new ItemStack(item) : null;
            }
            case 3,4 -> {
                Item item = PotionRecipeItemHandler.selectRecipeOfPathwayAndSequence(pathway, sequence);
                yield item != null ? new ItemStack(item) : null;
            }
            case 5,6 -> {
                Item item = BeyonderCharacteristicItemHandler.selectCharacteristicOfPathwayAndSequence(pathway, sequence);
                yield item != null ? new ItemStack(item) : null;
            }
            case 7 -> RitualManager.getRandomRitualBook();
            default -> {
                Item item = PotionItemHandler.selectPotionOfPathwayAndSequence(random, pathway, sequence);
                yield item != null ? new ItemStack(item) : null;
            }
        };
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC.get();
    }


}