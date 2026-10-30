package de.jakob.lotm.beyonders.singularity;

import de.jakob.lotm.LOTMCraft;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.ModelEvent;

/** Modelos extras das Singularidades usados só quando estão equipadas (diferentes do item na mão). */
public final class SingularityModels {
    private SingularityModels() {}

    public static final ModelResourceLocation FOOL_WORN = standalone("item/singularity/fool_worn");

    private static ModelResourceLocation standalone(String path) {
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(LOTMCraft.MOD_ID, path));
    }

    public static void register(ModelEvent.RegisterAdditional event) {
        event.register(FOOL_WORN);
    }
}
