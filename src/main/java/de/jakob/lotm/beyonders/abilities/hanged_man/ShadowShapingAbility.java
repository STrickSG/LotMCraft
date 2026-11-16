package de.jakob.lotm.beyonders.abilities.hanged_man;

import de.jakob.lotm.beyonders.abilities.core.Ability;
import de.jakob.lotm.damage.ModDamageTypes;
import de.jakob.lotm.util.helper.AbilityUtil;
import de.jakob.lotm.util.scheduling.ServerScheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Shadow Ascetic: shapes the shadows into a spear and hurls it. It flies until it hits a creature
 * or a block.
 */
public class ShadowShapingAbility extends Ability {
    private static final DustParticleOptions SHADOW = new DustParticleOptions(new Vector3f(0.06f, 0.02f, 0.08f), 1.6f);
    private static final DustParticleOptions EDGE = new DustParticleOptions(new Vector3f(0.45f, 0.03f, 0.08f), 0.9f);
    private static final double SPEED = 1.2;
    private static final int MAX_TICKS = 25;

    public ShadowShapingAbility(String id) {
        super(id, 6f, "darkness");
        baseDamage = 10;
    }

    @Override
    public Map<String, Integer> getRequirements() {
        return new HashMap<>(Map.of("hanged_man", 7));
    }

    @Override
    protected float getSpiritualityCost() {
        return 60;
    }

    @Override
    public void onAbilityUse(Level level, LivingEntity entity) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        Vec3 direction = entity.getLookAngle().normalize();
        // starts a little ahead so the particles do not cover the caster's view
        AtomicReference<Vec3> position = new AtomicReference<>(entity.getEyePosition().add(direction.scale(2.2)).subtract(0, 0.25, 0));
        AtomicBoolean done = new AtomicBoolean(false);
        float damage = baseDamage;
        level.playSound(null, entity.blockPosition(), SoundEvents.WARDEN_ATTACK_IMPACT, SoundSource.PLAYERS, 0.7f, 1.4f);

        ServerScheduler.scheduleForDuration(0, 1, MAX_TICKS, () -> {
            if (done.get()) return;
            Vec3 pos = position.get();
            for (int i = 0; i < 4; i++) {
                Vec3 p = pos.subtract(direction.scale(i * 0.3));
                if (p.distanceToSqr(entity.getEyePosition()) < 2.5) continue;
                serverLevel.sendParticles(SHADOW, p.x, p.y, p.z, 2, 0.04, 0.04, 0.04, 0);
            }
            serverLevel.sendParticles(EDGE, pos.x, pos.y, pos.z, 1, 0.08, 0.08, 0.08, 0);

            if (AbilityUtil.damageNearbyEntities(serverLevel, entity, 1.4f, ModDamageTypes.DARKNESS, damage, pos, true, false, true, 0)) {
                serverLevel.sendParticles(ParticleTypes.SQUID_INK, pos.x, pos.y, pos.z, 12, 0.3, 0.3, 0.3, 0.05);
                done.set(true);
                return;
            }
            if (!serverLevel.getBlockState(BlockPos.containing(pos)).getCollisionShape(serverLevel, BlockPos.containing(pos)).isEmpty()) {
                serverLevel.sendParticles(ParticleTypes.SQUID_INK, pos.x, pos.y, pos.z, 8, 0.2, 0.2, 0.2, 0.03);
                done.set(true);
                return;
            }
            position.set(pos.add(direction.scale(SPEED)));
        }, serverLevel);
    }
}
