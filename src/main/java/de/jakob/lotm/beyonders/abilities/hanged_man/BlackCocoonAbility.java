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
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

/**
 * Shadow Ascetic: wraps the target in a cocoon of black shadow. The target cannot move, jump or see
 * for a few seconds. Much stronger Beyonders break free almost at once.
 */
public class BlackCocoonAbility extends Ability {
    private static final DustParticleOptions SHADOW = new DustParticleOptions(new Vector3f(0.04f, 0.02f, 0.05f), 2.2f);

    public BlackCocoonAbility(String id) {
        super(id, 18f, "darkness");
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
        int duration = 20 * Math.min(6, 3 + Math.max(0, 7 - seq));
        if (BeyonderData.isBeyonder(target) && AbilityUtil.isTargetSignificantlyStronger(seq, BeyonderData.getSequence(target))) {
            duration = 10;
        }

        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, 6, false, false));
        target.addEffect(new MobEffectInstance(MobEffects.JUMP, duration, 250, false, false));   // negative jump: cannot jump
        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, duration, 0, false, false));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, 1, false, false));
        target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, duration, 2, false, false));
        level.playSound(null, target.blockPosition(), SoundEvents.SCULK_BLOCK_SPREAD, SoundSource.PLAYERS, 1.2f, 0.5f);

        ServerScheduler.scheduleForDuration(0, 3, duration, () -> {
            if (!target.isAlive()) return;
            double h = target.getBbHeight(), w = Math.max(0.35, target.getBbWidth() * 0.6);
            for (int i = 0; i < 14; i++) {
                double a = random.nextDouble() * Math.PI * 2, y = random.nextDouble() * h;
                double r = w * Math.sqrt(1 - Math.pow((y - h / 2) / (h / 2 + 0.2), 2));
                serverLevel.sendParticles(SHADOW, target.getX() + Math.cos(a) * r, target.getY() + y, target.getZ() + Math.sin(a) * r, 1, 0, 0, 0, 0);
            }
        }, serverLevel);
    }
}
