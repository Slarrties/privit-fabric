package dev.slarrties.privit.common.network.payload.s2c;

import dev.slarrties.privit.PrivitMod;
import dev.slarrties.privit.common.region.gui.state.RegionGuiState;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record RegionGuiInitS2CPacket(RegionGuiState state, boolean openGui) implements CustomPayload {

    public static final Id<RegionGuiInitS2CPacket> ID = new Id<>(PrivitMod.id("region_gui_init"));

    public static final PacketCodec<PacketByteBuf, RegionGuiInitS2CPacket> CODEC = PacketCodec.of(
            (value, buf) -> {
                value.state.writeToBuf(buf);
                buf.writeBoolean(value.openGui);
            },
            buf -> new RegionGuiInitS2CPacket(
                    RegionGuiState.readFromBuf(buf),
                    buf.readBoolean()
            )
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}