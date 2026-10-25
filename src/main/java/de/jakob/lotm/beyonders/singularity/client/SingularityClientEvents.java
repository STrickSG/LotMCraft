package de.jakob.lotm.beyonders.singularity.client;

import de.jakob.lotm.LOTMCraft;
import de.jakob.lotm.beyonders.singularity.SingularityHandler;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Efeitos visuais das Singularidades equipadas (só no cliente e só com o Curios instalado). */
@EventBusSubscriber(modid = LOTMCraft.MOD_ID, value = Dist.CLIENT)
public final class SingularityClientEvents {
    private SingularityClientEvents() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!SingularityHandler.curiosLoaded()) return;
        de.jakob.lotm.compat.curios.client.SunSingularityParticles.tick(Minecraft.getInstance());
    }
}
