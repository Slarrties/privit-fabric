package dev.slarrties.privit.common.network.payload.c2s;

import dev.slarrties.privit.PrivitMod;
import dev.slarrties.privit.common.network.payload.PrivitPacket;

import net.minecraft.util.Identifier;
import net.minecraft.network.PacketByteBuf;

import java.util.Set;
import java.util.UUID;
import java.util.HashSet;

public record RequestPlayerNamesC2SPacket(Set<UUID> uuids) implements PrivitPacket {
    public static final Identifier ID = PrivitMod.id("request_player_names");

    public RequestPlayerNamesC2SPacket {
        if (uuids.size() > 100) {
            throw new IllegalArgumentException("Too many UUIDs in one request");
        }
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeVarInt(uuids.size());

        for (UUID uuid : uuids) {
            buf.writeUuid(uuid);
        }
    }

    public static RequestPlayerNamesC2SPacket read(PacketByteBuf buf) {
        int size = buf.readVarInt();
        Set<UUID> uuids = new HashSet<>(size);

        for (int i = 0; i < size; i++) {
            uuids.add(buf.readUuid());
        }

        return new RequestPlayerNamesC2SPacket(uuids);
    }

    @Override
    public Identifier getId() {
        return ID;
    }
}