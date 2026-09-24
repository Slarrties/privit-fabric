package dev.slarrties.privit.common.network.payload.c2s;

import dev.slarrties.privit.PrivitMod;
import dev.slarrties.privit.common.network.payload.PrivitPacket;

import net.minecraft.util.Identifier;
import net.minecraft.network.PacketByteBuf;

public record SearchPlayersRequestC2SPacket(String query, int limit) implements PrivitPacket {
    public static final Identifier ID = PrivitMod.id("search_players");

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeString(query);
        buf.writeVarInt(limit);
    }

    public static SearchPlayersRequestC2SPacket read(PacketByteBuf buf) {
        return new SearchPlayersRequestC2SPacket(
                buf.readString(),
                buf.readVarInt()
        );
    }

    @Override
    public Identifier getId() {
        return ID;
    }
}