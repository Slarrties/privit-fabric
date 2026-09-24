package dev.slarrties.privit.common.network.payload.s2c;

import dev.slarrties.privit.PrivitMod;
import dev.slarrties.privit.common.network.payload.PrivitPacket;

import net.minecraft.util.Identifier;
import net.minecraft.network.PacketByteBuf;

import java.util.Map;
import java.util.UUID;
import java.util.LinkedHashMap;

public record PlayerSearchResultS2CPacket(Map<UUID, String> results) implements PrivitPacket {
    public static final Identifier ID = PrivitMod.id("player_search_result");

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeVarInt(results.size());
        results.forEach((uuid, name) -> {
            buf.writeUuid(uuid);
            buf.writeString(name);
        });
    }

    public static PlayerSearchResultS2CPacket read(PacketByteBuf buf) {
        int size = buf.readVarInt();
        Map<UUID, String> map = new LinkedHashMap<>();

        for (int i = 0; i < size; i++) {
            UUID uuid = buf.readUuid();
            String name = buf.readString();
            map.put(uuid, name);
        }

        return new PlayerSearchResultS2CPacket(map);
    }

    @Override
    public Identifier getId() {
        return ID;
    }
}