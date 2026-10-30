package de.jakob.lotm.addons.rituals;

import de.jakob.lotm.beyonders.potions.PotionItemHandler;
import de.jakob.lotm.beyonders.potions.BeyonderCharacteristicItemHandler;
import de.jakob.lotm.item.ModItems;
import de.jakob.lotm.util.BeyonderData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.List;
import java.util.function.Supplier;

/**
 * Rituais de avanço que já estão ligados e o que cada um precisa.
 * O pergaminho de ritual e a Carta da Blasfêmia mostram isso; a verificação fica em RitualChecks.
 */
public final class AdvancementRituals {
    private AdvancementRituals() {}

    /** Ritual para avançar até a Sequência {@code sequence} do Caminho {@code pathway}. */
    public record Ritual(String pathway, int sequence, Supplier<List<ItemStack>> materials) {
        /** Nome do pergaminho deste ritual, ex.: priest_of_light_ritual */
        public String id() {
            return BeyonderData.pathwayInfos.get(pathway).sequenceNames()[sequence] + "_ritual";
        }

        /** Começo das chaves de tradução com a explicação do ritual (…1, …2, …) */
        public String textKey() {
            return "lotmcraft.ritual." + pathway + "." + sequence + ".";
        }
    }

    private static final List<Ritual> RITUALS = List.of(
            // Sol, Sequência 5 (Sacerdote da Luz): em escuridão total, com o corpo enterrado em gelo que não derrete
            new Ritual("sun", 5, () -> List.of(
                    new ItemStack(Blocks.PACKED_ICE, 10),
                    new ItemStack(PotionItemHandler.PRIEST_OF_LIGHT_POTION.get()))),
            // Sol, Sequência 4 (Sem Sombra): extrair as emoções mais fortes antes e devolvê-las durante o avanço
            new Ritual("sun", 4, () -> List.of(
                    new ItemStack(Items.GLASS_BOTTLE),
                    EmotionFlask.example(),
                    new ItemStack(PotionItemHandler.UNSHADOWED.get()))),
            // Sol, Sequência 3 (Mentor da Justiça): definir a própria justiça e mantê-la sem nenhuma violação
            new Ritual("sun", 3, () -> List.of(
                    new ItemStack(Items.WRITABLE_BOOK),
                    new ItemStack(PotionItemHandler.JUSTICE_MENTOR_POTION.get()))),
            // Sol, Sequência 2 (Buscador da Luz): ligar a memória da sua justiça a um item de Anjo do Sol e pô-lo à sua frente
            new Ritual("sun", 2, () -> List.of(
                    new ItemStack(Items.WRITTEN_BOOK),
                    new ItemStack(BeyonderCharacteristicItemHandler.selectCharacteristicOfPathwayAndSequence("sun", 2)),
                    new ItemStack(PotionItemHandler.LIGHTSEEKER_POTION.get()))),
            // Sol, Sequência 1 (Anjo Branco): ser adorado por 100 fiéis, incluindo você mesmo
            new Ritual("sun", 1, () -> List.of(
                    new ItemStack(ModItems.SACRED_REGISTER.get()),
                    new ItemStack(Items.VILLAGER_SPAWN_EGG, 99),
                    new ItemStack(PotionItemHandler.WHITE_ANGEL_POTION.get())))
    );

    public static List<Ritual> all() {
        return RITUALS;
    }

    public static Ritual get(String pathway, int sequence) {
        for (Ritual ritual : RITUALS) {
            if (ritual.pathway().equals(pathway) && ritual.sequence() == sequence) return ritual;
        }
        return null;
    }
}
