package de.jakob.lotm.network.packets.toServer;

import de.jakob.lotm.LOTMCraft;
import de.jakob.lotm.attachments.ModAttachments;
import de.jakob.lotm.attachments.WaypointComponent;
import de.jakob.lotm.dimension.ModDimensions;
import de.jakob.lotm.particle.ModParticles;
import de.jakob.lotm.rendering.effectRendering.EffectIds;
import de.jakob.lotm.rendering.effectRendering.EffectManager;
import de.jakob.lotm.util.helper.AbilityUtil;
import de.jakob.lotm.util.helper.ParticleUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Set;

public record WaypointSelectedPacket(WaypointComponent.ClientWaypoint waypoint, String use) implements CustomPacketPayload {

    public static final Type<WaypointSelectedPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(LOTMCraft.MOD_ID, "waypoint_selected"));

    public static final StreamCodec<RegistryFriendlyByteBuf, WaypointSelectedPacket> STREAM_CODEC =
            StreamCodec.composite(
                    WaypointComponent.ClientWaypoint.STREAM_CODEC,
                    WaypointSelectedPacket::waypoint,
                    ByteBufCodecs.STRING_UTF8,
                    WaypointSelectedPacket::use,
                    WaypointSelectedPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(WaypointSelectedPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();

            if(!LOTMCraft.abilityHandler.getById("waypoint_ability").hasAbility(player, false)) {
                return;
            }

            ServerLevel serverLevel = player.serverLevel();
            MinecraftServer server = player.getServer();

            if (server == null) return;
            if(packet.use.equals("teleport")) {
                // Only teleport to a waypoint this player actually saved: the packet's coordinates and dimension
                // come from the client and used to be trusted as-is (teleport anywhere, or an NPE on a bad dimension).
                WaypointComponent waypointComponent = player.getData(ModAttachments.WAYPOINT_COMPONENT);
                WaypointComponent.Waypoint waypoint = waypointComponent.findByClientWaypoint(packet.waypoint, server);

                if(waypoint == null) {
                    return;
                }

                ServerLevel destinationLevel = waypoint.serverLevel();
                // Captured before teleporting, so one effect plays at the origin and one at the destination
                double originX = player.getX();
                double originY = player.getY();
                double originZ = player.getZ();

                serverLevel.playSound(null, originX, originY, originZ, SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 1, 1);
                player.teleportTo(destinationLevel, waypoint.x(), waypoint.y(), waypoint.z(), Set.of(), player.getYRot(), player.getXRot());
                destinationLevel.playSound(null, waypoint.x(), waypoint.y(), waypoint.z(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 1, 1);
                EffectManager.playEffect(EffectIds.WAYPOINT, waypoint.x(), waypoint.y() + 1, waypoint.z(), destinationLevel);
                EffectManager.playEffect(EffectIds.WAYPOINT, originX, originY + 1, originZ, serverLevel);
            }
            else if (packet.use.equals("delete")) {
                WaypointComponent waypointComponent = player.getData(ModAttachments.WAYPOINT_COMPONENT);
                WaypointComponent.Waypoint waypoint = waypointComponent.findByClientWaypoint(packet.waypoint, server);

                if(waypoint == null) {
                    return;
                }

                waypointComponent.deleteWaypoint(waypoint);
                AbilityUtil.sendActionBar(player, Component.translatable("ability.lotmcraft.waypoint.deleted").withColor(0x91f6ff));
            }
        });
    }

    public static ServerLevel resolveLevel(String levelKey, MinecraftServer server) {
        ResourceKey<Level> key = ResourceKey.create(
                Registries.DIMENSION,
                ResourceLocation.parse(levelKey)
        );
        return server.getLevel(key);
    }
}
