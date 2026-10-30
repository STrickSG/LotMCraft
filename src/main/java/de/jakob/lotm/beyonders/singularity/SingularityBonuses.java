package de.jakob.lotm.beyonders.singularity;

import de.jakob.lotm.LOTMCraft;
import de.jakob.lotm.beyonders.abilities.core.Ability;
import de.jakob.lotm.util.BeyonderData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

/**
 * Bônus do mod (não efeitos do Minecraft) que a Singularidade dá a quem é do mesmo Caminho dela:
 * ela representa o conceito do Caminho, então fortalece a espiritualidade, as habilidades e o corpo do portador.
 * Enquanto não está dominada, os bônus valem pela metade; dominada, valem inteiros.
 * Quem é de um Caminho vizinho não recebe esses bônus: ganha efeitos do Minecraft (SingularityEffects)
 * e uma habilidade alta emprestada do Caminho da Singularidade.
 * Caminhos sem bônus aqui continuam só com os efeitos de SingularityEffects.
 */
public final class SingularityBonuses {
    private SingularityBonuses() {}

    /**
     * Valores com a Singularidade dominada (frações: 0.30 = 30%).
     * @param maxSpirituality espiritualidade máxima a mais
     * @param sunlightRegen   regeneração de espiritualidade a mais sob a luz do dia
     * @param abilityCost     custo de espiritualidade a menos nas habilidades do Caminho
     * @param abilityCooldown recarga a menos nas habilidades do Caminho
     * @param maxHealth       vida máxima a mais
     */
    public record Bonuses(float maxSpirituality, float sunlightRegen, float abilityCost, float abilityCooldown, float maxHealth) {}

    private static final Map<String, Bonuses> BONUSES = Map.of(
            // Sol: luz que não se esgota e se renova sob o sol, corpo fortalecido pela vitalidade da luz
            "sun", new Bonuses(0.30f, 1.0f, 0.25f, 0.20f, 0.20f)
    );

    /** Habilidade alta do Caminho da Singularidade que ela empresta a quem é de um Caminho vizinho. */
    private static final Map<String, String> NEIGHBOR_ABILITY = Map.of(
            // Sol: Luz Branca Pura, do Anjo Branco (Sequência 1)
            "sun", "pure_white_light_ability"
    );

    public static String neighborAbility(String pathway) {
        return NEIGHBOR_ABILITY.get(pathway);
    }

    /** O portador é de um Caminho vizinho e a Singularidade equipada empresta esta habilidade a ele. */
    public static boolean grantsAbility(Player player, String abilityId) {
        for (ItemStack stack : SingularityHandler.equipped(player)) {
            if (SingularityData.canUse(stack, player) && !SingularityData.isOwnPathway(stack, player)
                    && abilityId.equals(NEIGHBOR_ABILITY.get(SingularityData.pathwayOf(stack)))) return true;
        }
        return false;
    }

    private static final ResourceLocation HEALTH_ID = ResourceLocation.fromNamespaceAndPath(LOTMCraft.MOD_ID, "singularity_health");

    public static boolean hasBonuses(String pathway) {
        return BONUSES.containsKey(pathway);
    }

    public static Bonuses of(String pathway) {
        return BONUSES.get(pathway);
    }

    /** 1 com a Singularidade do próprio Caminho dominada, 0.5 enquanto não dominada, 0 sem ela. */
    public static float strength(Player player) {
        if (player == null || !BeyonderData.isBeyonder(player) || !hasBonuses(BeyonderData.getPathway(player))) return 0f;
        float strength = 0f;
        for (ItemStack stack : SingularityHandler.equipped(player)) {
            if (!SingularityData.canUse(stack, player) || !SingularityData.isOwnPathway(stack, player)) continue;
            strength = Math.max(strength, SingularityData.isDominated(stack) ? 1f : 0.5f);
        }
        return strength;
    }

    private static Bonuses active(Player player) {
        return BONUSES.get(BeyonderData.getPathway(player));
    }

    public static float maxSpiritualityFactor(Player player) {
        float strength = strength(player);
        return strength == 0f ? 1f : 1f + active(player).maxSpirituality() * strength;
    }

    /** Fator do custo de espiritualidade de uma habilidade do próprio Caminho. */
    public static float costFactor(LivingEntity entity, Ability ability) {
        if (!(entity instanceof Player player) || !ownPathwayAbility(player, ability)) return 1f;
        float strength = strength(player);
        return strength == 0f ? 1f : 1f - active(player).abilityCost() * strength;
    }

    /** Fator da recarga de uma habilidade do próprio Caminho. */
    public static float cooldownFactor(LivingEntity entity, Ability ability) {
        if (!(entity instanceof Player player) || !ownPathwayAbility(player, ability)) return 1f;
        float strength = strength(player);
        return strength == 0f ? 1f : 1f - active(player).abilityCooldown() * strength;
    }

    private static boolean ownPathwayAbility(Player player, Ability ability) {
        return ability.getRequirements().containsKey(BeyonderData.getPathway(player));
    }

    /** Chamado uma vez por segundo: vida máxima, espiritualidade sob o sol e limite de espiritualidade. */
    public static void tick(ServerPlayer player) {
        float strength = strength(player);
        Bonuses bonuses = strength == 0f ? null : active(player);

        AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
        double wanted = bonuses == null ? 0 : bonuses.maxHealth() * strength;
        AttributeModifier current = health == null ? null : health.getModifier(HEALTH_ID);
        if (health != null && (current == null ? wanted != 0 : current.amount() != wanted)) {
            health.removeModifier(HEALTH_ID);
            if (wanted != 0) health.addPermanentModifier(new AttributeModifier(HEALTH_ID, wanted, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
        }

        if (!BeyonderData.isBeyonder(player)) return;
        float max = BeyonderData.getMaxSpirituality(BeyonderData.getPathway(player, true), BeyonderData.getSequence(player, false, true), player);
        // sem a Singularidade, a espiritualidade que passou do limite volta ao normal
        if (BeyonderData.getSpirituality(player) > max) BeyonderData.setSpirituality(player, max);

        // a luz do dia renova a espiritualidade (a regeneração normal é 0.0006 do máximo por tick)
        if (bonuses != null && bonuses.sunlightRegen() > 0 && player.level().isDay() && !player.level().isRaining()
                && player.level().canSeeSky(player.blockPosition().above())) {
            BeyonderData.incrementSpirituality(player, max * 0.0006f * 20 * bonuses.sunlightRegen() * strength);
        }
    }
}
