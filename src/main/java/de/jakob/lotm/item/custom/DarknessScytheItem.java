package de.jakob.lotm.item.custom;

import de.jakob.lotm.beyonders.singularity.SingularityData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import org.jetbrains.annotations.NotNull;

/**
 * Singularidade da Escuridão em forma de foice: também é uma arma.
 * Nas mãos do dono, cada golpe corta em área e envolve os alvos em escuridão.
 */
public class DarknessScytheItem extends UniquenessItem {

    public DarknessScytheItem(Properties properties) {
        super(properties.attributes(SwordItem.createAttributes(Tiers.NETHERITE, 6, -2.8f)), "darkness");
    }

    @Override
    public boolean hurtEnemy(@NotNull ItemStack stack, @NotNull LivingEntity target, @NotNull LivingEntity attacker) {
        if (attacker.level() instanceof ServerLevel level && attacker instanceof Player player && SingularityData.isHolder(stack, player)) {
            target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0));
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
            // Corte em arco: atinge também quem está perto do alvo
            for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(2.5),
                    e -> e != attacker && e != target && e.isAlive() && !e.isAlliedTo(attacker))) {
                other.hurt(level.damageSources().playerAttack(player), 6f);
                other.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 40, 0));
            }
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY(0.5), target.getZ(), 1, 0, 0, 0, 0);
            level.sendParticles(ParticleTypes.SQUID_INK, target.getX(), target.getY(0.5), target.getZ(), 12, .6, .4, .6, .02);
            level.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1f, 0.6f);
        }
        return true;
    }
}
