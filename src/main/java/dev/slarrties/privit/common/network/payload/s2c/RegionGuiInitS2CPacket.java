package dev.slarrties.privit.common.network.payload.s2c;

import dev.slarrties.privit.PrivitMod;
import dev.slarrties.privit.common.network.payload.PrivitPacket;
import dev.slarrties.privit.common.region.gui.state.RegionGuiState;

import net.minecraft.util.Identifier;
import net.minecraft.network.PacketByteBuf;

public record RegionGuiInitS2CPacket(RegionGuiState state, boolean openGui) implements PrivitPacket {
    public static final Identifier ID = PrivitMod.id("region_gui_init");

    @Override
    public void write(PacketByteBuf buf) {
        state.writeToBuf(buf);
        buf.writeBoolean(openGui);
    }

    public static RegionGuiInitS2CPacket read(PacketByteBuf buf) {
        return new RegionGuiInitS2CPacket(
                RegionGuiState.readFromBuf(buf),
                buf.readBoolean()
        );
    }

    @Override
    public Identifier getId() {
        return ID;
    }
}