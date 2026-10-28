package dev.slarrties.privit.common.network.payload.s2c;

import dev.slarrties.privit.PrivitMod;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.Map;
import java.util.UUID;
import java.util.LinkedHashMap;

public record PlayerSearchResultS2CPacket(Map<UUID, String> results) implements CustomPacketPayload {
    public static final Type<PlayerSearchResultS2CPacket> TYPE = new Type<>(PrivitMod.id("player_search_result"));

    public static final StreamCodec<FriendlyByteBuf, PlayerSearchResultS2CPacket> CODEC = StreamCodec.of(
            (FriendlyByteBuf buf, PlayerSearchResultS2CPacket packet) -> {
                buf.writeVarInt(packet.results.size());
                packet.results.forEach((uuid, name) -> {
                    buf.writeUUID(uuid);
                    buf.writeUtf(name);
                });
            },
            buf -> {
                int size = buf.readVarInt();
                Map<UUID, String> map = new LinkedHashMap<>();
                for (int i = 0; i < size; i++) {
                    UUID uuid = buf.readUUID();
                    String name = buf.readUtf();
                    map.put(uuid, name);
                }
                return new PlayerSearchResultS2CPacket(map);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}