package de.jakob.lotm.beyonders.abilities.hanged_man;

import de.jakob.lotm.beyonders.abilities.core.Ability;
import de.jakob.lotm.util.BeyonderData;
import de.jakob.lotm.util.helper.AbilityUtil;
import de.jakob.lotm.util.scheduling.ServerScheduler;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

/**
 * Shadow Ascetic: seizes the target's shadow. The shadow drags its owner towards the ascetic and then
 * holds them in place for a moment.
 */
public class ShadowSeizureAbility extends Ability {
    private static final DustParticleOptions SHADOW = new DustParticleOptions(new Vector3f(0.05f, 0.02f, 0.06f), 1.8f);
    private static final int PULL_TICKS = 30;

    public ShadowSeizureAbility(String id) {
        super(id, 15f, "darkness");
    }

    @Override
    public Map<String, Integer> getRequirements() {
        return new HashMap<>(Map.of("hanged_man", 7));
    }

    @Override
    protected float getSpiritualityCost() {
        return 50;
    }

    @Override
    public void onAbilityUse(Level level, LivingEntity entity) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        LivingEntity target = AbilityUtil.getTargetEntity(entity, Math.max(10, baseDistance), 2f);
        if (target == null) {
            AbilityUtil.sendActionBar(entity, Component.translatable("ability.lotmcraft.hanged_man.no_target").withColor(0xFFaaaaaa));
            return;
        }

        int seq = AbilityUtil.getSeqWithArt(entity, this);
        boolean stronger = BeyonderData.isBeyonder(target) && AbilityUtil.isTargetSignificantlyStronger(seq, BeyonderData.getSequence(target));
        double strength = stronger ? 0.12 : 0.35;
        level.playSound(null, target.blockPosition(), SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.PLAYERS, 0.5f, 1.6f);

        ServerScheduler.scheduleForDuration(0, 1, PULL_TICKS, () -> {
            if (!target.isAlive() || !entity.isAlive() || target.level() != entity.level()) return;
            Vec3 toCaster = entity.position().subtract(target.position());
            if (toCaster.lengthSqr() > 4) {
                Vec3 pull = toCaster.normalize().scale(strength);
                target.setDeltaMovement(pull.x, Math.max(target.getDeltaMovement().y, -0.1), pull.z);
                target.hurtMarked = true;
            }
            // the seized shadow, stretched on the ground between the two
            Vec3 a = target.position(), b = entity.position();
            for (int i = 0; i < 6; i++) {
                Vec3 p = a.lerp(b, random.nextDouble());
                serverLevel.sendParticles(SHADOW, p.x, p.y + 0.05, p.z, 1, 0.1, 0, 0.1, 0);
            }
        }, () -> {
            if (target.isAlive() && !stronger) {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 6, false, false));
                target.addEffect(new MobEffectInstance(MobEffects.JUMP, 30, 250, false, false));
            }
        }, serverLevel);
    }
}
