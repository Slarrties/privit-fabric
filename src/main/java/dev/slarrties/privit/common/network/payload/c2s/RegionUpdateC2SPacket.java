package dev.slarrties.privit.common.network.payload.c2s;

import dev.slarrties.privit.PrivitMod;
import dev.slarrties.privit.common.region.gui.state.RegionGuiState;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record RegionUpdateC2SPacket(RegionGuiState state) implements CustomPacketPayload {
    public static final Type<RegionUpdateC2SPacket> TYPE = new Type<>(PrivitMod.id("update_region"));
    public static final StreamCodec<FriendlyByteBuf, RegionUpdateC2SPacket> CODEC = StreamCodec.of(
            (FriendlyByteBuf buf, RegionUpdateC2SPacket packet) -> packet.state.writeToBuf(buf),
            buf -> new RegionUpdateC2SPacket(RegionGuiState.readFromBuf(buf))
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}