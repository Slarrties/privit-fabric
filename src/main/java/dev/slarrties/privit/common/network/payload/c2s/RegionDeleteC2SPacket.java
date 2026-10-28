package dev.slarrties.privit.common.network.payload.c2s;

import dev.slarrties.privit.PrivitMod;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record RegionDeleteC2SPacket(UUID regionId) implements CustomPacketPayload {
    public static final Type<RegionDeleteC2SPacket> TYPE = new Type<>(PrivitMod.id("delete_region"));
    public static final StreamCodec<FriendlyByteBuf, RegionDeleteC2SPacket> CODEC = StreamCodec.of(
            (FriendlyByteBuf buf, RegionDeleteC2SPacket packet) -> buf.writeUUID(packet.regionId),
            buf -> new RegionDeleteC2SPacket(buf.readUUID())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}