package de.jakob.lotm.item;

import net.minecraft.world.item.Item;

public class PotionIngredient extends Item {
    private final int sequence;
    private final String[] pathways;
    private final boolean isMainIngredient;

    public PotionIngredient(Properties properties, int sequence, boolean isMainIngredient, String... pathways) {
        super(properties);
        this.sequence = sequence;
        this.isMainIngredient = isMainIngredient;
        this.pathways = pathways;
    }

    public int getSequence() {
        return sequence;
    }

    public String[] getPathways() {
        return pathways;
    }

    /**
     * Most ingredients list their pathways as one comma separated string ("red_priest,death,sun"), so
     * Arrays.asList(getPathways()).contains(pathway) never matched an ingredient shared by several pathways.
     */
    public boolean isForPathway(String pathway) {
        if (pathways == null) return false;
        for (String entry : pathways) {
            for (String p : entry.split(",")) {
                if (p.trim().equals(pathway)) return true;
            }
        }
        return false;
    }

    public boolean isMainIngredient() {
        return isMainIngredient;
    }
}
