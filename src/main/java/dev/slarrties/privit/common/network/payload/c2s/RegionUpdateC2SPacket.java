package dev.slarrties.privit.common.network.payload.c2s;

import dev.slarrties.privit.PrivitMod;
import dev.slarrties.privit.common.network.payload.PrivitPacket;
import dev.slarrties.privit.common.region.gui.state.RegionGuiState;

import net.minecraft.util.Identifier;
import net.minecraft.network.PacketByteBuf;

public record RegionUpdateC2SPacket(RegionGuiState state) implements PrivitPacket {
    public static final Identifier ID = PrivitMod.id("update_region");

    @Override
    public void write(PacketByteBuf buf) {
        state.writeToBuf(buf);
    }

    public static RegionUpdateC2SPacket read(PacketByteBuf buf) {
        return new RegionUpdateC2SPacket(RegionGuiState.readFromBuf(buf));
    }

    @Override
    public Identifier getId() {
        return ID;
    }
}