package dev.slarrties.privit.common.network.payload.c2s;

import dev.slarrties.privit.PrivitMod;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

import java.util.Set;
import java.util.UUID;
import java.util.HashSet;

public record RequestPlayerNamesC2SPacket(Set<UUID> uuids) implements CustomPayload {

    public static final Id<RequestPlayerNamesC2SPacket> ID = new Id<>(PrivitMod.id("request_player_names"));

    public static final PacketCodec<PacketByteBuf, RequestPlayerNamesC2SPacket> CODEC = PacketCodec.of(
            RequestPlayerNamesC2SPacket::write,
            RequestPlayerNamesC2SPacket::read
    );

    public RequestPlayerNamesC2SPacket {
        if (uuids.size() > 100) {
            throw new IllegalArgumentException("Too many UUIDs in one request");
        }
    }

    private void write(PacketByteBuf buf) {
        buf.writeVarInt(uuids.size());

        for (UUID uuid : uuids) {
            buf.writeUuid(uuid);
        }
    }

    private static RequestPlayerNamesC2SPacket read(PacketByteBuf buf) {
        int size = buf.readVarInt();
        Set<UUID> uuids = new HashSet<>(size);

        for (int i = 0; i < size; i++) {
            uuids.add(buf.readUuid());
        }

        return new RequestPlayerNamesC2SPacket(uuids);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}