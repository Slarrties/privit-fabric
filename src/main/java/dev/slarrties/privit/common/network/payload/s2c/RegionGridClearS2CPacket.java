package dev.slarrties.privit.common.network.payload.s2c;

import dev.slarrties.privit.PrivitMod;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record RegionGridClearS2CPacket() implements CustomPacketPayload {
    public static final Type<RegionGridClearS2CPacket> TYPE = new Type<>(PrivitMod.id("region_grid_clear_s2c"));

    public static final StreamCodec<FriendlyByteBuf, RegionGridClearS2CPacket> CODEC = StreamCodec.of(
            (FriendlyByteBuf buf, RegionGridClearS2CPacket packet) -> {},
            buf -> new RegionGridClearS2CPacket()
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}