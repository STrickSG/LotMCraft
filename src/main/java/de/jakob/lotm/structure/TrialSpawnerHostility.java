package de.jakob.lotm.structure;

import de.jakob.lotm.LOTMCraft;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/**
 * Os espíritos do mod só se defendem (atacam quem bate neles). Os que nascem dos Spawners de Desafios das
 * estruturas das cartas vêm com a marca HOSTILE_TAG (no "spawn_potentials" do spawner) e atacam o jogador
 * mais perto. Os espíritos de fora dos spawners continuam como sempre.
 */
@EventBusSubscriber(modid = LOTMCraft.MOD_ID)
public final class TrialSpawnerHostility {
    public static final String HOSTILE_TAG = "lotmcraft_trial_hostile";

    private TrialSpawnerHostility() {}

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Mob mob) || !mob.getTags().contains(HOSTILE_TAG)) return;
        // os objetivos não são salvos: ao carregar o chunk de novo o espírito entra outra vez e ganha de novo
        mob.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(mob, Player.class, false));
    }
}
