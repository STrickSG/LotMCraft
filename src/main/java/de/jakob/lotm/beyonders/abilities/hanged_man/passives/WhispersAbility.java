package de.jakob.lotm.beyonders.abilities.hanged_man.passives;

import de.jakob.lotm.LOTMCraft;
import de.jakob.lotm.beyonders.abilities.core.PassiveAbility;
import de.jakob.lotm.util.helper.AbilityUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Listener: the secret whispers never stop. When a creature that is hunting the Listener comes close
 * without being seen (behind a wall or behind them), the whispers warn where it is.
 */
@EventBusSubscriber(modid = LOTMCraft.MOD_ID)
public class WhispersAbility extends PassiveAbility {
    private static final double RADIUS = 24;
    private static final long WARN_COOLDOWN_TICKS = 20 * 10;
    private static final Map<UUID, Long> lastWarning = new ConcurrentHashMap<>();

    public WhispersAbility(String id) {
        super(id);
    }

    @Override
    public Map<String, Integer> getRequirements() {
        return new HashMap<>(Map.of("hanged_man", 8));
    }

    @Override
    public void tick(Level level, LivingEntity entity) {
        if (!(level instanceof ServerLevel serverLevel) || !(entity instanceof ServerPlayer player) || entity.tickCount % 20 != 0) return;

        long now = level.getGameTime();
        Long last = lastWarning.get(player.getUUID());
        if (last != null && now - last < WARN_COOLDOWN_TICKS && now >= last) return;

        Vec3 look = player.getLookAngle();
        Mob hunter = serverLevel.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(RADIUS),
                        m -> m.isAlive() && m.getTarget() == player
                                && (!player.hasLineOfSight(m) || m.position().subtract(player.position()).normalize().dot(look) < 0))
                .stream().min(Comparator.comparingDouble(m -> m.distanceToSqr(player))).orElse(null);
        if (hunter == null) return;

        lastWarning.put(player.getUUID(), now);
        AbilityUtil.sendActionBar(player, Component.translatable("ability.lotmcraft.whispers.warning",
                Component.translatable("ability.lotmcraft.whispers.direction." + direction(player, hunter))).withColor(0xFF8a0a0a));
        player.playNotifySound(SoundEvents.AMBIENT_CAVE.value(), SoundSource.AMBIENT, 0.5f, 1.5f);
    }

    /** behind / front / left / right, relative to where the player looks */
    private static String direction(ServerPlayer player, Mob hunter) {
        Vec3 to = hunter.position().subtract(player.position()).multiply(1, 0, 1).normalize();
        Vec3 look = player.getLookAngle().multiply(1, 0, 1).normalize();
        double forward = to.dot(look);
        double side = look.x * to.z - look.z * to.x;   // > 0: to the right
        if (forward < -0.5) return "behind";
        if (forward > 0.5) return "front";
        return side > 0 ? "right" : "left";
    }

    // Game times of the old world must not carry over to the next singleplayer world
    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        lastWarning.clear();
    }
}
