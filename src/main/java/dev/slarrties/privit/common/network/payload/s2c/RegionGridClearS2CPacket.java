package dev.slarrties.privit.common.network.payload.s2c;

import dev.slarrties.privit.PrivitMod;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record RegionGridClearS2CPacket() implements CustomPayload {

    public static final Id<RegionGridClearS2CPacket> ID = new Id<>(PrivitMod.id("region_grid_clear_s2c"));

    public static final PacketCodec<PacketByteBuf, RegionGridClearS2CPacket> CODEC =
            PacketCodec.of((value, buf) -> {}, buf -> new RegionGridClearS2CPacket());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}