package dev.slarrties.privit.common.network.payload.c2s;

import dev.slarrties.privit.PrivitMod;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

import java.util.UUID;

public record RegionDeleteC2SPacket(UUID regionId) implements CustomPayload {

    public static final Id<RegionDeleteC2SPacket> ID = new Id<>(PrivitMod.id("delete_region"));

    public static final PacketCodec<PacketByteBuf, RegionDeleteC2SPacket> CODEC = PacketCodec.of(
            (value, buf) -> buf.writeUuid(value.regionId()),
            buf -> new RegionDeleteC2SPacket(buf.readUuid())
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}