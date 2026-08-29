package dev.slarrties.privit.common.network.payload.c2s;

import dev.slarrties.privit.PrivitMod;
import dev.slarrties.privit.common.region.gui.state.RegionGuiState;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record RegionUpdateC2SPacket(RegionGuiState state) implements CustomPayload {

    public static final Id<RegionUpdateC2SPacket> ID = new Id<>(PrivitMod.id("update_region"));
    public static final PacketCodec<PacketByteBuf, RegionUpdateC2SPacket> CODEC = PacketCodec.of(
            (value, buf) -> value.state.writeToBuf(buf),
            buf -> new RegionUpdateC2SPacket(RegionGuiState.readFromBuf(buf))
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}