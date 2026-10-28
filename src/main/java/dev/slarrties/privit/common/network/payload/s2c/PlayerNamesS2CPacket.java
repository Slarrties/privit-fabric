package dev.slarrties.privit.common.network.payload.s2c;

import dev.slarrties.privit.PrivitMod;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.Map;
import java.util.UUID;
import java.util.HashMap;

public record PlayerNamesS2CPacket(Map<UUID, String> names) implements CustomPacketPayload {
    public static final Type<PlayerNamesS2CPacket> TYPE = new Type<>(PrivitMod.id("player_names"));

    public static final StreamCodec<FriendlyByteBuf, PlayerNamesS2CPacket> CODEC = StreamCodec.of(
            (FriendlyByteBuf buf, PlayerNamesS2CPacket packet) -> packet.write(buf),
            PlayerNamesS2CPacket::read
    );

    private void write(FriendlyByteBuf buf) {
        buf.writeVarInt(names.size());
        for (var entry : names.entrySet()) {
            buf.writeUUID(entry.getKey());
            buf.writeUtf(entry.getValue(), 64);
        }
    }

    private static PlayerNamesS2CPacket read(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        Map<UUID, String> names = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            UUID uuid = buf.readUUID();
            String name = buf.readUtf(64);
            names.put(uuid, name);
        }
        return new PlayerNamesS2CPacket(names);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}