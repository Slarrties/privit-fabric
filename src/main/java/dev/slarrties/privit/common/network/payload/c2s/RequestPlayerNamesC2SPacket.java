package dev.slarrties.privit.common.network.payload.c2s;

import dev.slarrties.privit.PrivitMod;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.Set;
import java.util.UUID;
import java.util.HashSet;

public record RequestPlayerNamesC2SPacket(Set<UUID> uuids) implements CustomPacketPayload {
    public static final Type<RequestPlayerNamesC2SPacket> TYPE = new Type<>(PrivitMod.id("request_player_names"));

    public static final StreamCodec<FriendlyByteBuf, RequestPlayerNamesC2SPacket> CODEC = StreamCodec.of(
            (FriendlyByteBuf buf, RequestPlayerNamesC2SPacket packet) -> packet.write(buf),
            RequestPlayerNamesC2SPacket::read
    );

    public RequestPlayerNamesC2SPacket {
        if (uuids.size() > 100) {
            throw new IllegalArgumentException("Too many UUIDs in one request");
        }
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeVarInt(uuids.size());
        for (UUID uuid : uuids) {
            buf.writeUUID(uuid);
        }
    }

    private static RequestPlayerNamesC2SPacket read(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        Set<UUID> uuids = new HashSet<>(size);
        for (int i = 0; i < size; i++) {
            uuids.add(buf.readUUID());
        }
        return new RequestPlayerNamesC2SPacket(uuids);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}