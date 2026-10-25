package de.jakob.lotm.beyonders.singularity.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.player.Player;

/** Quem está lendo a descrição e se está segurando Shift (só no cliente). */
public final class SingularityTooltipClient {
    private SingularityTooltipClient() {}

    public static Player viewer() {
        return Minecraft.getInstance().player;
    }

    public static boolean expanded() {
        return Screen.hasShiftDown();
    }
}
