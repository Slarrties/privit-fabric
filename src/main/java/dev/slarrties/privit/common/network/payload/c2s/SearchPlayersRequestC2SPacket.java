package dev.slarrties.privit.common.network.payload.c2s;

import dev.slarrties.privit.PrivitMod;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record SearchPlayersRequestC2SPacket(String query, int limit) implements CustomPayload {

    public static final Id<SearchPlayersRequestC2SPacket> ID = new Id<>(PrivitMod.id("search_players"));

    public static final PacketCodec<PacketByteBuf, SearchPlayersRequestC2SPacket> CODEC = PacketCodec.of(
            (value, buf) -> {
                buf.writeString(value.query);
                buf.writeVarInt(value.limit);
            },
            buf -> new SearchPlayersRequestC2SPacket(
                    buf.readString(),
                    buf.readVarInt()
            )
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}