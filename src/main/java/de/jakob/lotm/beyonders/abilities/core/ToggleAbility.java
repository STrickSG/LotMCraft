package de.jakob.lotm.beyonders.abilities.core;

import de.jakob.lotm.LOTMCraft;
import de.jakob.lotm.network.PacketHandler;
import de.jakob.lotm.network.packets.toClient.SyncToggleAbilityPacket;
import de.jakob.lotm.util.BeyonderData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public abstract class ToggleAbility extends Ability {

    private static final HashMap<UUID, HashSet<ToggleAbility>> activeAbilities = new HashMap<>();
    private static final HashMap<UUID, HashSet<ToggleAbility>> activeAbilitiesClientCache = new HashMap<>();


    public int tickRate = 5;

    protected ToggleAbility(String id, String... interactionFlags) {
        super(id, 0, interactionFlags);

        doesNotIncreaseDigestion = true;
    }

    @Override
    public void onAbilityUse(Level level, LivingEntity entity) {
        if(level.isClientSide) {
            return;
        }

        if(!activeAbilities.containsKey(entity.getUUID()) || !activeAbilities.get(entity.getUUID()).contains(this)) {
            activeAbilities.putIfAbsent(entity.getUUID(), new HashSet<>());
            activeAbilities.get(entity.getUUID()).add(this);
            start(level, entity);
            PacketHandler.sendToAllPlayersInSameLevel(new SyncToggleAbilityPacket(entity.getId(), getId(), SyncToggleAbilityPacket.Action.START.getValue()), (ServerLevel) level);
            return;
        }

        cancel((ServerLevel) level, entity);
    }

    protected void cancel(ServerLevel level, LivingEntity entity) {
        if(activeAbilities.containsKey(entity.getUUID())) {
            activeAbilities.get(entity.getUUID()).remove(this);
        }
        stop(level, entity);
        PacketHandler.sendToAllPlayersInSameLevel(new SyncToggleAbilityPacket(entity.getId(), getId(), SyncToggleAbilityPacket.Action.STOP.getValue()), level);
    }

    public static void cleanUp(ServerLevel serverLevel, LivingEntity entity) {
        if(!activeAbilities.containsKey(entity.getUUID())) {
            return;
        }

        (new HashSet<>(activeAbilities.get(entity.getUUID()))).forEach(toggleAbility -> {
            toggleAbility.cancel(serverLevel, entity);
        });
    }

    public void prepareTick(Level level, LivingEntity entity) {
        if(!level.isClientSide && shouldConsumeSpirituality(entity)) {
            float cost = getSpiritualityCost();
            if(BeyonderData.getSpirituality(entity) <= cost) {
                cancel((ServerLevel) level, entity);
                return;
            }

            BeyonderData.reduceSpirituality(entity, cost);
        }

        if(!level.isClientSide && entity instanceof Mob mob && mob.getTarget() ==null) {
            cancel((ServerLevel) level, entity);
            return;
        }

        tick(level, entity);
        if(level instanceof ServerLevel serverLevel) NeoForge.EVENT_BUS.post(new AbilityUsedEvent(serverLevel, entity.position(), entity, this, interactionFlags, interactionRadius, interactionCacheTicks));
    }

    public abstract void tick(Level level, LivingEntity entity);
    public abstract void start(Level level, LivingEntity entity);
    public abstract void stop(Level level, LivingEntity entity);

    public boolean isActiveForEntity(LivingEntity entity) {
        if(!entity.level().isClientSide) {
            return activeAbilities.containsKey(entity.getUUID()) && activeAbilities.get(entity.getUUID()).contains(this);
        }
        else {
            // Must read the client cache: the server map is empty on a remote client
            HashSet<ToggleAbility> cached = activeAbilitiesClientCache.get(entity.getUUID());
            return cached != null && cached.contains(this);
        }
    }

    public void updateClientCache(LivingEntity entity, boolean active) {
        if(!entity.level().isClientSide) {
            return;
        }

        activeAbilitiesClientCache.putIfAbsent(entity.getUUID(), new HashSet<>());
        if(active) {
            activeAbilitiesClientCache.get(entity.getUUID()).add(this);
        }
        else {
            activeAbilitiesClientCache.get(entity.getUUID()).remove(this);
        }

    }

    public static Set<ToggleAbility> getActiveAbilitiesForEntity(LivingEntity entity) {
        if(!activeAbilities.containsKey(entity.getUUID())) {
            return Collections.emptySet();
        }
        return new HashSet<>(activeAbilities.get(entity.getUUID()));
    }

    public static void setActiveAbilities(LivingEntity entity, HashSet<ToggleAbility> newAbilities) {
        // clear the old ones
        cleanUp((ServerLevel) entity.level(), entity);

        activeAbilities.put(entity.getUUID(), newAbilities);
    }

    @EventBusSubscriber(modid = LOTMCraft.MOD_ID)
    public static class LifecycleHandler {
        @SubscribeEvent
        public static void onServerStopped(ServerStoppedEvent event) {
            // Same behaviour as a dedicated server restart: nothing stays toggled on. In singleplayer this
            // map outlives the integrated server and kept the old world's toggles ticking in the next one.
            activeAbilities.clear();
        }
    }

    /** Client side: forget toggle states of the world being left (called on logout). */
    public static void clearClientCache() {
        activeAbilitiesClientCache.clear();
    }
}
