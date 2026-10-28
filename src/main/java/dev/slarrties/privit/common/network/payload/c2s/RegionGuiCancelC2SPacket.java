package dev.slarrties.privit.common.network.payload.c2s;

import dev.slarrties.privit.PrivitMod;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record RegionGuiCancelC2SPacket(UUID regionId) implements CustomPacketPayload {
    public static final Type<RegionGuiCancelC2SPacket> TYPE = new Type<>(PrivitMod.id("region_gui_cancel_c2s"));
    public static final StreamCodec<FriendlyByteBuf, RegionGuiCancelC2SPacket> CODEC = StreamCodec.of(
            (FriendlyByteBuf buf, RegionGuiCancelC2SPacket packet) -> buf.writeUUID(packet.regionId),
            buf -> new RegionGuiCancelC2SPacket(buf.readUUID())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}