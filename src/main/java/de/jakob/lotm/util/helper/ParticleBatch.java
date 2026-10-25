package de.jakob.lotm.util.helper;

import de.jakob.lotm.network.packets.toClient.ParticleBatchPacket;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;

/**
 * Junta várias partículas do mesmo tipo e manda num pacote só para cada jogador,
 * em vez de um pacote por partícula (o que um laço de level.sendParticles faz).
 * O resultado no cliente é o mesmo: cada jogador recebe só as partículas a até 32 blocos, como no Minecraft.
 */
public final class ParticleBatch {
    private static final double RANGE = 32.0;
    private final ServerLevel level;
    private final ParticleOptions particle;
    private double[] data = new double[7 * 16];
    private int[] counts = new int[16];
    private int size = 0;

    public ParticleBatch(ServerLevel level, ParticleOptions particle) {
        this.level = level;
        this.particle = particle;
    }

    /** Mesmos argumentos de level.sendParticles depois do tipo de partícula. */
    public void add(double x, double y, double z, int count, double dx, double dy, double dz, double speed) {
        if (size == counts.length) {
            counts = Arrays.copyOf(counts, size * 2);
            data = Arrays.copyOf(data, size * 2 * 7);
        }
        int i = size * 7;
        data[i] = x; data[i + 1] = y; data[i + 2] = z; data[i + 3] = dx; data[i + 4] = dy; data[i + 5] = dz; data[i + 6] = speed;
        counts[size++] = count;
    }

    public void send() {
        if (level == null || size == 0) return;
        for (ServerPlayer player : level.players()) {
            double[] mine = null;
            int[] mineCounts = null;
            int n = 0;
            for (int e = 0; e < size; e++) {
                int i = e * 7;
                if (!player.blockPosition().closerToCenterThan(new Vec3(data[i], data[i + 1], data[i + 2]), RANGE)) continue;
                if (mine == null) { mine = new double[size * 7]; mineCounts = new int[size]; }
                System.arraycopy(data, i, mine, n * 7, 7);
                mineCounts[n++] = counts[e];
            }
            if (n > 0) player.connection.send(new ParticleBatchPacket(particle, Arrays.copyOf(mine, n * 7), Arrays.copyOf(mineCounts, n)));
        }
        size = 0;
    }
}
