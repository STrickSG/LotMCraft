package de.jakob.lotm.beyonders.abilities.hanged_man;

import de.jakob.lotm.beyonders.abilities.core.ToggleAbility;
import de.jakob.lotm.util.helper.AbilityUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

/**
 * Shadow Ascetic: hides inside the shadows. While active and standing in darkness (light level 7 or
 * lower) the ascetic is invisible; in bright light the shadows cannot hold them.
 */
public class ShadowHidingAbility extends ToggleAbility {
    private static final int MAX_LIGHT = 7;

    public ShadowHidingAbility(String id) {
        super(id, "darkness");
    }

    @Override
    public Map<String, Integer> getRequirements() {
        return new HashMap<>(Map.of("hanged_man", 7));
    }

    @Override
    protected float getSpiritualityCost() {
        return 2;
    }

    private static boolean inShadow(Level level, LivingEntity entity) {
        return level.getMaxLocalRawBrightness(entity.blockPosition()) <= MAX_LIGHT;
    }

    private static void hide(LivingEntity entity) {
        entity.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 30, 0, false, false, true));
    }

    /** Only removes the short invisibility given by this ability, never a longer one (potion). */
    private static void reveal(LivingEntity entity) {
        MobEffectInstance current = entity.getEffect(MobEffects.INVISIBILITY);
        if (current != null && current.getDuration() <= 30) entity.removeEffect(MobEffects.INVISIBILITY);
    }

    @Override
    public void start(Level level, LivingEntity entity) {
        if (level.isClientSide) return;
        AbilityUtil.sendActionBar(entity, Component.translatable("ability.lotmcraft.shadow_hiding.start").withColor(0xFF8a0a0a));
    }

    @Override
    public void tick(Level level, LivingEntity entity) {
        if (!(level instanceof ServerLevel)) return;
        if (inShadow(level, entity)) {
            hide(entity);
        } else {
            reveal(entity);
            if (entity.tickCount % 40 == 0) {
                AbilityUtil.sendActionBar(entity, Component.translatable("ability.lotmcraft.shadow_hiding.too_bright").withColor(0xFFaaaaaa));
            }
        }
    }

    @Override
    public void stop(Level level, LivingEntity entity) {
        if (level.isClientSide) return;
        reveal(entity);
    }
}
