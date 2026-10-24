package de.jakob.lotm.compat.curios.client;

import de.jakob.lotm.beyonders.singularity.SingularityItems;
import net.minecraft.world.item.Item;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;

/** Registra o desenho das Singularidades no corpo (só no cliente e só com o Curios instalado). */
public final class CuriosClientSetup {
    private CuriosClientSetup() {}

    public static void registerRenderers() {
        for (Item item : SingularityItems.all()) {
            CuriosRendererRegistry.register(item, SingularityCurioRenderer::new);
        }
    }
}
