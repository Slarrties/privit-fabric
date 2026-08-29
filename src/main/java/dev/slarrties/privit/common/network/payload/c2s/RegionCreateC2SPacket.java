package dev.slarrties.privit.common.network.payload.c2s;

import dev.slarrties.privit.PrivitMod;
import dev.slarrties.privit.common.region.gui.state.RegionGuiState;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record RegionCreateC2SPacket(RegionGuiState state) implements CustomPayload {

    public static final Id<RegionCreateC2SPacket> ID = new Id<>(PrivitMod.id("create_region"));
    public static final PacketCodec<PacketByteBuf, RegionCreateC2SPacket> CODEC = PacketCodec.of(
            (value, buf) -> value.state.writeToBuf(buf),
            buf -> new RegionCreateC2SPacket(RegionGuiState.readFromBuf(buf))
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}