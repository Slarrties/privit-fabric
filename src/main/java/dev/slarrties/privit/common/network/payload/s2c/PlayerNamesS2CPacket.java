package dev.slarrties.privit.common.network.payload.s2c;

import dev.slarrties.privit.PrivitMod;
import dev.slarrties.privit.common.network.payload.PrivitPacket;

import net.minecraft.util.Identifier;
import net.minecraft.network.PacketByteBuf;

import java.util.Map;
import java.util.UUID;
import java.util.HashMap;

public record PlayerNamesS2CPacket(Map<UUID, String> names) implements PrivitPacket {
    public static final Identifier ID = PrivitMod.id("player_names");

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeVarInt(names.size());
        for (var entry : names.entrySet()) {
            buf.writeUuid(entry.getKey());
            buf.writeString(entry.getValue(), 64);
        }
    }

    public static PlayerNamesS2CPacket read(PacketByteBuf buf) {
        int size = buf.readVarInt();
        Map<UUID, String> names = new HashMap<>(size);

        for (int i = 0; i < size; i++) {
            UUID uuid = buf.readUuid();
            String name = buf.readString(64);
            names.put(uuid, name);
        }

        return new PlayerNamesS2CPacket(names);
    }

    @Override
    public Identifier getId() {
        return ID;
    }
}