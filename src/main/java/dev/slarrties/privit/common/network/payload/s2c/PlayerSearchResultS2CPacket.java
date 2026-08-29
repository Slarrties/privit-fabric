package dev.slarrties.privit.common.network.payload.s2c;

import dev.slarrties.privit.PrivitMod;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

import java.util.Map;
import java.util.UUID;
import java.util.LinkedHashMap;

public record PlayerSearchResultS2CPacket(Map<UUID, String> results) implements CustomPayload {

    public static final Id<PlayerSearchResultS2CPacket> ID = new Id<>(PrivitMod.id("player_search_result"));

    public static final PacketCodec<PacketByteBuf, PlayerSearchResultS2CPacket> CODEC = PacketCodec.of(
            (value, buf) -> {
                buf.writeVarInt(value.results.size());
                value.results.forEach((uuid, name) -> {
                    buf.writeUuid(uuid);
                    buf.writeString(name);
                });
            },
            buf -> {
                int size = buf.readVarInt();
                Map<UUID, String> map = new LinkedHashMap<>();
                for (int i = 0; i < size; i++) {
                    UUID uuid = buf.readUuid();
                    String name = buf.readString();
                    map.put(uuid, name);
                }
                return new PlayerSearchResultS2CPacket(map);
            }
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}