package dev.slarrties.privit.common.network.payload.s2c;

import dev.slarrties.privit.PrivitMod;
import dev.slarrties.privit.common.region.gui.state.RegionGuiState;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record RegionGuiInitS2CPacket(RegionGuiState state, boolean openGui) implements CustomPacketPayload {
    public static final Type<RegionGuiInitS2CPacket> TYPE = new Type<>(PrivitMod.id("region_gui_init"));

    public static final StreamCodec<FriendlyByteBuf, RegionGuiInitS2CPacket> CODEC = StreamCodec.of(
            (FriendlyByteBuf buf, RegionGuiInitS2CPacket packet) -> {
                packet.state.writeToBuf(buf);
                buf.writeBoolean(packet.openGui);
            },
            buf -> new RegionGuiInitS2CPacket(
                    RegionGuiState.readFromBuf(buf),
                    buf.readBoolean()
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}