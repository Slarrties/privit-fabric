package dev.slarrties.privit.common.network.payload.s2c;

import dev.slarrties.privit.PrivitMod;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record RegionGuiCloseS2CPacket(UUID regionId) implements CustomPacketPayload {
    public static final Type<RegionGuiCloseS2CPacket> TYPE = new Type<>(PrivitMod.id("region_gui_close_s2c"));

    public static final StreamCodec<FriendlyByteBuf, RegionGuiCloseS2CPacket> CODEC = StreamCodec.of(
            (FriendlyByteBuf buf, RegionGuiCloseS2CPacket packet) -> buf.writeUUID(packet.regionId),
            buf -> new RegionGuiCloseS2CPacket(buf.readUUID())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}