package de.jakob.lotm.beyonders.abilities.hanged_man;

import de.jakob.lotm.attachments.ModAttachments;
import de.jakob.lotm.attachments.SanityComponent;
import de.jakob.lotm.beyonders.abilities.core.Ability;
import de.jakob.lotm.util.helper.AbilityUtil;
import de.jakob.lotm.util.helper.ParticleUtil;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

/**
 * Secrets Supplicant: prays to the secret entities. They answer with a borrowed fragment of power
 * (strength and resistance for a while), but sometimes their whispers shake the supplicant's mind.
 */
public class SecretPrayerAbility extends Ability {
    private static final DustParticleOptions DUST = new DustParticleOptions(new Vector3f(0.45f, 0.03f, 0.05f), 1.4f);

    public SecretPrayerAbility(String id) {
        super(id, 90f);
    }

    @Override
    public Map<String, Integer> getRequirements() {
        return new HashMap<>(Map.of("hanged_man", 9));
    }

    @Override
    protected float getSpiritualityCost() {
        return 40;
    }

    @Override
    public void onAbilityUse(Level level, LivingEntity entity) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        int seq = AbilityUtil.getSeqWithArt(entity, this);
        int duration = 20 * 40;
        entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, seq <= 7 ? 1 : 0));
        entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 0));

        ParticleUtil.spawnParticles(serverLevel, DUST, entity.position().add(0, 1, 0), 40, 0.6, 0.02);
        level.playSound(null, entity.blockPosition(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 1f, 0.6f);

        // The secret entities do not answer for free
        if (entity instanceof Player && random.nextFloat() < 0.25f) {
            SanityComponent sanity = entity.getData(ModAttachments.SANITY_COMPONENT);
            sanity.setSanityAndSync(Math.max(0f, sanity.getSanity() - 0.03f), entity);
            entity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 20 * 5, 0));
            AbilityUtil.sendActionBar(entity, Component.translatable("ability.lotmcraft.secret_prayer.whispers").withColor(0xFF8a0a0a));
        } else {
            AbilityUtil.sendActionBar(entity, Component.translatable("ability.lotmcraft.secret_prayer.answered").withColor(0xFFc0392b));
        }
    }
}
