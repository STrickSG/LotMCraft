package de.jakob.lotm.beyonders.singularity;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Efeitos de cada Singularidade equipada.
 * Positivos: sempre ativos enquanto equipada pelo dono.
 * Negativos: só enquanto ela não estiver dominada (disparam a cada {@code negativeIntervalSeconds}).
 */
public final class SingularityEffects {
    private SingularityEffects() {}

    public record Positive(Holder<MobEffect> effect, int amplifier) {}

    public record Definition(List<Positive> positives, int negativeIntervalSeconds, Consumer<ServerPlayer> negative) {}

    private static Positive p(Holder<MobEffect> effect, int amplifier) {
        return new Positive(effect, amplifier);
    }

    private static Consumer<ServerPlayer> give(Holder<MobEffect> effect, int seconds, int amplifier) {
        return player -> player.addEffect(new MobEffectInstance(effect, seconds * 20, amplifier));
    }

    private static final Map<String, Definition> DEFINITIONS = Map.ofEntries(
            // Máscara do Tolo: regeneração e resistência; fragmentos de personalidade causam náusea
            Map.entry("fool", new Definition(List.of(p(MobEffects.REGENERATION, 0), p(MobEffects.DAMAGE_RESISTANCE, 0)), 45,
                    give(MobEffects.CONFUSION, 8, 0))),
            // Monóculo de Cristal: percepção espiritual (revela quem está por perto); o Verme do Tempo consome você
            Map.entry("error", new Definition(List.of(p(MobEffects.NIGHT_VISION, 0)), 30,
                    player -> {
                        player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 10 * 20, 1));
                        player.giveExperiencePoints(-5);
                    })),
            // Olhos de Cristal: leveza e velocidade; o espaço se desloca e te teletransporta sem aviso
            Map.entry("door", new Definition(List.of(p(MobEffects.MOVEMENT_SPEED, 0), p(MobEffects.SLOW_FALLING, 0)), 40,
                    SingularityEffects::randomShortTeleport)),
            // Visionário: mente afiada; alucinações escurecem a visão
            Map.entry("visionary", new Definition(List.of(p(MobEffects.DIG_SPEED, 0), p(MobEffects.NIGHT_VISION, 0)), 40,
                    give(MobEffects.DARKNESS, 6, 0))),
            // Sol: resistência ao fogo e regeneração; a luz não deixa você se esconder
            Map.entry("sun", new Definition(List.of(p(MobEffects.FIRE_RESISTANCE, 0), p(MobEffects.REGENERATION, 0)), 30,
                    give(MobEffects.GLOWING, 20, 0))),
            // Tirano: domínio sobre os mares; raios caem ao seu redor
            Map.entry("tyrant", new Definition(List.of(p(MobEffects.WATER_BREATHING, 0), p(MobEffects.DOLPHINS_GRACE, 0), p(MobEffects.DAMAGE_BOOST, 0)), 60,
                    SingularityEffects::lightningNearby)),
            // Escuridão: visão na noite e proteção; o sono pesa sobre você
            Map.entry("darkness", new Definition(List.of(p(MobEffects.NIGHT_VISION, 0), p(MobEffects.DAMAGE_RESISTANCE, 0)), 40,
                    give(MobEffects.MOVEMENT_SLOWDOWN, 10, 1))),
            // Demônia: agilidade; a catástrofe envenena o corpo
            Map.entry("demoness", new Definition(List.of(p(MobEffects.MOVEMENT_SPEED, 1)), 35,
                    give(MobEffects.POISON, 5, 0))),
            // Estandarte de Sangue de Salinger: força e resistência ao fogo; a guerra cobra fome
            Map.entry("red_priest", new Definition(List.of(p(MobEffects.DAMAGE_BOOST, 1), p(MobEffects.FIRE_RESISTANCE, 0)), 30,
                    give(MobEffects.HUNGER, 15, 1))),
            // Mãe: vida e saciedade; raízes prendem seus passos
            Map.entry("mother", new Definition(List.of(p(MobEffects.REGENERATION, 0), p(MobEffects.SATURATION, 0)), 40,
                    give(MobEffects.MOVEMENT_SLOWDOWN, 8, 2))),
            // Abismo: força e resistência ao fogo; a corrupção apodrece a carne
            Map.entry("abyss", new Definition(List.of(p(MobEffects.DAMAGE_BOOST, 1), p(MobEffects.FIRE_RESISTANCE, 0)), 35,
                    give(MobEffects.WITHER, 5, 0))),
            // Dados da Probabilidade: muita sorte; às vezes o destino vira contra você
            Map.entry("wheel_of_fortune", new Definition(List.of(p(MobEffects.LUCK, 2)), 30,
                    give(MobEffects.UNLUCK, 20, 1))),
            // Justiceiro: ordem e proteção; as leis prendem seus movimentos
            Map.entry("justiciar", new Definition(List.of(p(MobEffects.DAMAGE_RESISTANCE, 1)), 35,
                    give(MobEffects.DIG_SLOWDOWN, 15, 1)))
    );

    public static Definition get(String pathway) {
        return DEFINITIONS.get(pathway);
    }

    private static void randomShortTeleport(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        for (int attempt = 0; attempt < 16; attempt++) {
            double x = player.getX() + (player.getRandom().nextDouble() - .5) * 16;
            double y = player.getY() + player.getRandom().nextInt(8) - 4;
            double z = player.getZ() + (player.getRandom().nextDouble() - .5) * 16;
            if (player.randomTeleport(x, y, z, true)) {
                level.playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT,
                        net.minecraft.sounds.SoundSource.PLAYERS, 0.8f, 1.2f);
                return;
            }
        }
    }

    private static void lightningNearby(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt == null) return;
        Vec3 pos = player.position().add((player.getRandom().nextDouble() - .5) * 6, 0, (player.getRandom().nextDouble() - .5) * 6);
        bolt.moveTo(pos);
        level.addFreshEntity(bolt);
    }

    /** Aplica (ou renova) os efeitos positivos; duração curta para sumirem logo após desequipar. */
    public static void applyPositives(LivingEntity entity, Definition definition) {
        for (Positive positive : definition.positives()) {
            MobEffectInstance current = entity.getEffect(positive.effect());
            if (current == null || current.getDuration() < 60 || current.getAmplifier() < positive.amplifier()) {
                // Visão noturna pisca abaixo de 10 segundos, então ela fica com folga maior
                int duration = positive.effect() == MobEffects.NIGHT_VISION ? 300 : 100;
                entity.addEffect(new MobEffectInstance(positive.effect(), duration, positive.amplifier(), true, false, true));
            }
        }
    }
}
