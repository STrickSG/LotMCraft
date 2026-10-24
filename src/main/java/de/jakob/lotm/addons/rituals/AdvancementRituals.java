package de.jakob.lotm.addons.rituals;

import de.jakob.lotm.beyonders.potions.PotionItemHandler;
import de.jakob.lotm.util.BeyonderData;
import net.minecraft.world.item.ItemStack;
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
            // Sol, Sequência 5 (Sacerdote da Luz): beber a poção em escuridão total, enterrado em gelo compactado
            new Ritual("sun", 5, () -> List.of(
                    new ItemStack(Blocks.PACKED_ICE, 10),
                    new ItemStack(PotionItemHandler.PRIEST_OF_LIGHT_POTION.get())))
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
