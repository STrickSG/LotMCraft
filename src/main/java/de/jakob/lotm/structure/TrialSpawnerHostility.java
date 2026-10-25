package de.jakob.lotm.structure;

import de.jakob.lotm.LOTMCraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Os espíritos do mod só se defendem (atacam quem bate neles) e são feitos para Beyonders de Sequência alta.
 * Os que nascem dos Spawners de Desafios das estruturas das cartas vêm com marcas (no "spawn_potentials" do spawner):
 * HOSTILE_TAG faz o espírito atacar o jogador mais perto, e "lotmcraft_trial_hp:0.33" / "lotmcraft_trial_dmg:0.13"
 * multiplicam a vida e o dano dele, para o desafio crescer andar por andar sem matar com um golpe.
 * Os espíritos de fora dos spawners não mudam.
 */
@EventBusSubscriber(modid = LOTMCraft.MOD_ID)
public final class TrialSpawnerHostility {
    public static final String HOSTILE_TAG = "lotmcraft_trial_hostile";
    private static final String HP_TAG = "lotmcraft_trial_hp:", DMG_TAG = "lotmcraft_trial_dmg:", SCALED_TAG = "lotmcraft_trial_scaled";
    private static final ResourceLocation HEALTH_ID = ResourceLocation.fromNamespaceAndPath(LOTMCraft.MOD_ID, "trial_health");

    private TrialSpawnerHostility() {}

    private static float factor(Mob mob, String prefix) {
        for (String tag : mob.getTags()) {
            if (tag.startsWith(prefix)) {
                try { return Float.parseFloat(tag.substring(prefix.length())); } catch (NumberFormatException ignored) { }
            }
        }
        return 1f;
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Mob mob) || !mob.getTags().contains(HOSTILE_TAG)) return;
        // os objetivos não são salvos: ao carregar o chunk de novo o espírito entra outra vez e ganha de novo
        mob.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(mob, Player.class, false));
        float hp = factor(mob, HP_TAG);
        AttributeInstance health = mob.getAttribute(Attributes.MAX_HEALTH);
        if (hp != 1f && health != null) {
            // modificador com ID fixo: ao recarregar não acumula
            health.addOrUpdateTransientModifier(new AttributeModifier(HEALTH_ID, hp - 1f, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            if (mob.addTag(SCALED_TAG)) mob.setHealth(mob.getMaxHealth());         // só na primeira vez
            else if (mob.getHealth() > mob.getMaxHealth()) mob.setHealth(mob.getMaxHealth());
        }
    }

    @SubscribeEvent
    public static void onDamage(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof Mob mob) || !mob.getTags().contains(HOSTILE_TAG)) return;
        float dmg = factor(mob, DMG_TAG);
        if (dmg != 1f) event.setAmount(event.getAmount() * dmg);
    }
}
