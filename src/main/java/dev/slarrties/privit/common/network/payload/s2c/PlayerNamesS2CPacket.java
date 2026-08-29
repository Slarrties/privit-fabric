package dev.slarrties.privit.common.network.payload.s2c;

import dev.slarrties.privit.PrivitMod;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

import java.util.Map;
import java.util.UUID;
import java.util.HashMap;

public record PlayerNamesS2CPacket(Map<UUID, String> names) implements CustomPayload {

    public static final Id<PlayerNamesS2CPacket> ID = new Id<>(PrivitMod.id("player_names"));

    public static final PacketCodec<PacketByteBuf, PlayerNamesS2CPacket> CODEC = PacketCodec.of(
            PlayerNamesS2CPacket::write,
            PlayerNamesS2CPacket::read
    );

    private void write(PacketByteBuf buf) {
        buf.writeVarInt(names.size());
        for (var entry : names.entrySet()) {
            buf.writeUuid(entry.getKey());
            buf.writeString(entry.getValue(), 64);
        }
    }

    private static PlayerNamesS2CPacket read(PacketByteBuf buf) {
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
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}