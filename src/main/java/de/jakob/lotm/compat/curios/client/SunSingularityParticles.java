package de.jakob.lotm.compat.curios.client;

import de.jakob.lotm.beyonders.singularity.SingularityData;
import de.jakob.lotm.beyonders.singularity.SingularityHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Partículas de luz do sol em miniatura: faíscas douradas, brilhos alaranjados e pequenas brasas saindo dele. */
public final class SunSingularityParticles {
    private SunSingularityParticles() {}

    private static final DustParticleOptions GOLD = new DustParticleOptions(new Vector3f(1f, 0.72f, 0.18f), 0.35f);
    private static final DustParticleOptions LIGHT_GOLD = new DustParticleOptions(new Vector3f(1f, 0.88f, 0.42f), 0.28f);

    public static void tick(Minecraft mc) {
        if (mc.level == null || mc.isPaused()) return;
        RandomSource random = mc.level.random;
        for (Player player : mc.level.players()) {
            if (player.isInvisible() || !wearsSun(player)) continue;
            // em primeira pessoa você não vê o próprio sol, então as partículas também não aparecem
            if (player == mc.getCameraEntity() && mc.options.getCameraType().isFirstPerson()) continue;

            Vec3 pos = SingularityCurioRenderer.shoulderWorldPos(player, 1f);
            if (random.nextFloat() < 0.25f) {
                mc.level.addParticle(random.nextBoolean() ? GOLD : LIGHT_GOLD,
                        pos.x + (random.nextDouble() - .5) * 0.25, pos.y + (random.nextDouble() - .5) * 0.25, pos.z + (random.nextDouble() - .5) * 0.25,
                        0, 0.02, 0);
            }
            if (random.nextFloat() < 0.05f) {
                mc.level.addParticle(ParticleTypes.WAX_ON,
                        pos.x + (random.nextDouble() - .5) * 0.3, pos.y + (random.nextDouble() - .5) * 0.3, pos.z + (random.nextDouble() - .5) * 0.3,
                        0, 0, 0);
            }
            if (random.nextFloat() < 0.03f) {
                mc.level.addParticle(ParticleTypes.SMALL_FLAME,
                        pos.x + (random.nextDouble() - .5) * 0.2, pos.y - 0.05, pos.z + (random.nextDouble() - .5) * 0.2,
                        0, -0.01, 0);
            }
        }
    }

    private static boolean wearsSun(Player player) {
        for (ItemStack stack : SingularityHandler.equipped(player)) {
            if ("sun".equals(SingularityData.pathwayOf(stack))) return true;
        }
        return false;
    }
}
