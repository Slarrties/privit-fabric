package dev.slarrties.privit.common.network.payload.c2s;

import dev.slarrties.privit.PrivitMod;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record SearchPlayersRequestC2SPacket(String query, int limit) implements CustomPacketPayload {
    public static final Type<SearchPlayersRequestC2SPacket> TYPE = new Type<>(PrivitMod.id("search_players"));
    public static final StreamCodec<FriendlyByteBuf, SearchPlayersRequestC2SPacket> CODEC = StreamCodec.of(
            (FriendlyByteBuf buf, SearchPlayersRequestC2SPacket packet) -> {
                buf.writeUtf(packet.query);
                buf.writeVarInt(packet.limit);
            },
            buf -> new SearchPlayersRequestC2SPacket(
                    buf.readUtf(),
                    buf.readVarInt()
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}