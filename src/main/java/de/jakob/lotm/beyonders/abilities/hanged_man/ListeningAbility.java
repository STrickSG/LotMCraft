package de.jakob.lotm.beyonders.abilities.hanged_man;

import de.jakob.lotm.beyonders.abilities.core.Ability;
import de.jakob.lotm.beyonders.abilities.visionary.handlers.VisionaryHandler;
import de.jakob.lotm.util.BeyonderData;
import de.jakob.lotm.util.helper.AbilityUtil;
import de.jakob.lotm.util.scheduling.ServerScheduler;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Listener: listens closely for a while. Living beings around (even behind walls) are marked with
 * particles that only the listener sees. Beyonders that are far stronger, or hidden by Visionary
 * powers, cannot be heard.
 */
public class ListeningAbility extends Ability {
    private static final double RADIUS = 32;
    private static final DustParticleOptions CREATURE = new DustParticleOptions(new Vector3f(0.75f, 0.75f, 0.8f), 1.2f);
    private static final DustParticleOptions BEYONDER = new DustParticleOptions(new Vector3f(0.75f, 0.05f, 0.08f), 1.5f);

    public ListeningAbility(String id) {
        super(id, 20f);
        canBeUsedByNPC = false;
    }

    @Override
    public Map<String, Integer> getRequirements() {
        return new HashMap<>(Map.of("hanged_man", 8));
    }

    @Override
    protected float getSpiritualityCost() {
        return 30;
    }

    private List<LivingEntity> heard(ServerLevel level, LivingEntity listener, int seq) {
        return AbilityUtil.getNearbyEntities(listener, level, listener.position(), RADIUS, true).stream()
                .filter(e -> !VisionaryHandler.shouldStayInvisible(seq, e))
                .filter(e -> !BeyonderData.isBeyonder(e) || !AbilityUtil.isTargetSignificantlyStronger(seq, BeyonderData.getSequence(e)))
                .toList();
    }

    @Override
    public void onAbilityUse(Level level, LivingEntity entity) {
        if (!(level instanceof ServerLevel serverLevel) || !(entity instanceof ServerPlayer player)) return;

        int seq = AbilityUtil.getSeqWithArt(entity, this);
        level.playSound(null, entity.blockPosition(), SoundEvents.SCULK_CLICKING, SoundSource.PLAYERS, 0.6f, 0.7f);

        List<LivingEntity> first = heard(serverLevel, entity, seq);
        long beyonders = first.stream().filter(BeyonderData::isBeyonder).count();
        AbilityUtil.sendActionBar(entity, Component.translatable("ability.lotmcraft.listening.result",
                first.size() - beyonders, beyonders).withColor(0xFFc0392b));

        ServerScheduler.scheduleForDuration(0, 10, 20 * 10, () -> {
            if (!player.isAlive() || player.level() != serverLevel) return;
            for (LivingEntity e : heard(serverLevel, player, seq)) {
                // sent only to the listener
                serverLevel.sendParticles(player, BeyonderData.isBeyonder(e) ? BEYONDER : CREATURE, true,
                        e.getX(), e.getY() + e.getBbHeight() + 0.4, e.getZ(), 4, 0.15, 0.15, 0.15, 0);
            }
        }, serverLevel);
    }
}
