package de.jakob.lotm.events;

import de.jakob.lotm.LOTMCraft;
import de.jakob.lotm.beyonders.abilities.core.interaction.InteractionHandler;
import de.jakob.lotm.attachments.DoorAuthorityData;
import de.jakob.lotm.attachments.SealedDimensionData;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = LOTMCraft.MOD_ID)
public class GlobalTickHandler {

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if(event.getLevel().isClientSide()) {
            return;
        }

        // Stored in each level's own data storage, so every level ticks its own copy
        DoorAuthorityData doorData = DoorAuthorityData.get((ServerLevel) event.getLevel());

        if (doorData.isActive()) {
            doorData.tick();
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        // Once per server tick instead of once per loaded level
        InteractionHandler.cleanupInteractions();

        // A single instance stored in the overworld and shared by all levels. It used to be ticked from the
        // level tick, i.e. once per loaded dimension (15 of them), so a 5 minute seal ran out after ~20 seconds.
        SealedDimensionData sealedDimensionData = SealedDimensionData.get(event.getServer().overworld());

        if (sealedDimensionData.isActive()) {
            sealedDimensionData.tick();
        }
    }

}
