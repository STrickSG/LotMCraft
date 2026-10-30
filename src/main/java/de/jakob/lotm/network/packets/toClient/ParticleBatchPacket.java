package de.jakob.lotm.network.packets.toClient;

import de.jakob.lotm.LOTMCraft;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Várias chamadas de "sendParticles" do mesmo tipo de partícula num pacote só (ver ParticleBatch).
 * Cada entrada tem 7 números (x, y, z, dx, dy, dz, velocidade) e uma contagem; o cliente faz com cada
 * entrada exatamente o que o Minecraft faz com um pacote de partículas comum.
 */
public record ParticleBatchPacket(ParticleOptions particle, double[] data, int[] counts) implements CustomPacketPayload {
    public static final Type<ParticleBatchPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(LOTMCraft.MOD_ID, "particle_batch"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ParticleBatchPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                ParticleTypes.STREAM_CODEC.encode(buf, packet.particle());
                buf.writeVarInt(packet.counts().length);
                for (int i = 0; i < packet.counts().length; i++) {
                    buf.writeDouble(packet.data()[i * 7]);
                    buf.writeDouble(packet.data()[i * 7 + 1]);
                    buf.writeDouble(packet.data()[i * 7 + 2]);
                    for (int k = 3; k < 7; k++) buf.writeFloat((float) packet.data()[i * 7 + k]);
                    buf.writeVarInt(packet.counts()[i]);
                }
            },
            buf -> {
                ParticleOptions particle = ParticleTypes.STREAM_CODEC.decode(buf);
                int n = buf.readVarInt();
                double[] data = new double[n * 7];
                int[] counts = new int[n];
                for (int i = 0; i < n; i++) {
                    data[i * 7] = buf.readDouble();
                    data[i * 7 + 1] = buf.readDouble();
                    data[i * 7 + 2] = buf.readDouble();
                    for (int k = 3; k < 7; k++) data[i * 7 + k] = buf.readFloat();
                    counts[i] = buf.readVarInt();
                }
                return new ParticleBatchPacket(particle, data, counts);
            });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ParticleBatchPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.flow().isClientbound()) Client.spawn(packet);
        });
    }

    /** Separado para a classe do cliente não ser carregada no servidor dedicado. */
    private static final class Client {
        // igual ao ClientPacketListener.handleParticleEvent do Minecraft, para cada entrada
        static void spawn(ParticleBatchPacket packet) {
            var level = Minecraft.getInstance().level;
            if (level == null) return;
            RandomSource random = level.random;
            double[] d = packet.data();
            for (int i = 0; i < packet.counts().length; i++) {
                double x = d[i * 7], y = d[i * 7 + 1], z = d[i * 7 + 2];
                double dx = d[i * 7 + 3], dy = d[i * 7 + 4], dz = d[i * 7 + 5], speed = d[i * 7 + 6];
                int count = packet.counts()[i];
                try {
                    if (count == 0) {
                        level.addParticle(packet.particle(), false, x, y, z, speed * dx, speed * dy, speed * dz);
                    } else {
                        for (int k = 0; k < count; k++) {
                            double ox = random.nextGaussian() * dx, oy = random.nextGaussian() * dy, oz = random.nextGaussian() * dz;
                            double vx = random.nextGaussian() * speed, vy = random.nextGaussian() * speed, vz = random.nextGaussian() * speed;
                            level.addParticle(packet.particle(), false, x + ox, y + oy, z + oz, vx, vy, vz);
                        }
                    }
                } catch (Throwable t) {
                    LOTMCraft.LOGGER.warn("Could not spawn particle effect {}", packet.particle());
                    return;
                }
            }
        }
    }
}
