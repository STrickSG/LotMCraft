package de.jakob.lotm.network.packets.toServer;

import de.jakob.lotm.LOTMCraft;
import de.jakob.lotm.beyonders.abilities.visionary.prophecy.TokenStream;
import de.jakob.lotm.util.BeyonderData;
import de.jakob.lotm.util.playerMap.HonorificName;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.LinkedList;

/**
 * Sent by the client when the player sets their honorific name via the in-menu UI.
 * The server validates the lines (same rules as /honorificname set) and saves the name.
 */
public record SetHonorificNamePacket(LinkedList<String> lines) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SetHonorificNamePacket> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(LOTMCraft.MOD_ID, "set_honorific_name"));

    public static final StreamCodec<FriendlyByteBuf, SetHonorificNamePacket> STREAM_CODEC =
            StreamCodec.of(
                    (buf, pkt) -> {
                        buf.writeVarInt(pkt.lines().size());
                        for (String line : pkt.lines()) {
                            buf.writeUtf(line, HonorificName.MAX_LENGTH);
                        }
                    },
                    buf -> {
                        int size = buf.readVarInt();
                        LinkedList<String> list = new LinkedList<>();
                        for (int i = 0; i < size; i++) {
                            list.add(buf.readUtf(HonorificName.MAX_LENGTH));
                        }
                        return new SetHonorificNamePacket(list);
                    }
            );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SetHonorificNamePacket pkt, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!context.flow().getReceptionSide().isServer()) return;

            ServerPlayer player = (ServerPlayer) context.player();
            LinkedList<String> lines = pkt.lines();

            try {
                int sequence = BeyonderData.getSequence(player);

                if (sequence >= 4) {
                    player.sendSystemMessage(Component.translatable("lotmcraft.msg.honorific.need_seq3")
                            .withStyle(ChatFormatting.RED));
                    return;
                }

                if (sequence == 3 && lines.size() != 5) {
                    player.sendSystemMessage(Component.translatable("lotmcraft.msg.honorific.five_lines")
                            .withStyle(ChatFormatting.RED));
                    return;
                }

                if (sequence == 2 && lines.size() < 4) {
                    player.sendSystemMessage(Component.translatable("lotmcraft.msg.honorific.four_lines")
                            .withStyle(ChatFormatting.RED));
                    return;
                }

                if (lines.size() < 3) {
                    player.sendSystemMessage(Component.translatable("lotmcraft.msg.honorific.min_lines")
                            .withStyle(ChatFormatting.RED));
                    return;
                }

                if (lines.stream().distinct().count() != lines.size()) {
                    player.sendSystemMessage(Component.translatable("lotmcraft.msg.honorific.different_lines")
                            .withStyle(ChatFormatting.RED));
                    return;
                }

                for (String line : lines) {
                    if (line.length() >= HonorificName.MAX_LENGTH) {
                        player.sendSystemMessage(Component.translatable("lotmcraft.msg.honorific.max_length", HonorificName.MAX_LENGTH)
                                .withStyle(ChatFormatting.RED));
                        return;
                    }

                    var stream = new TokenStream(line);
                    if(stream.getTotalSize() < 3){
                        player.sendSystemMessage(Component.translatable("lotmcraft.msg.honorific.min_words")
                                .withStyle(ChatFormatting.RED));
                        return;
                    }
                }

                String pathway = BeyonderData.getPathway(player);
                if (!HonorificName.validate(pathway, lines)) {
                    player.sendSystemMessage(Component.translatable("lotmcraft.msg.honorific.must_have_words", HonorificName.getMustHaveWords(pathway))
                            .withStyle(ChatFormatting.RED));
                    return;
                }

                BeyonderData.setHonorificName(player, new HonorificName(lines));
                player.sendSystemMessage(Component.translatable("lotmcraft.msg.honorific.set_success")
                        .withStyle(ChatFormatting.GREEN));

            } catch (Exception e) {
                player.sendSystemMessage(Component.translatable("lotmcraft.msg.honorific.set_failed", e.getMessage())
                        .withStyle(ChatFormatting.RED));
            }
        });
    }
}
