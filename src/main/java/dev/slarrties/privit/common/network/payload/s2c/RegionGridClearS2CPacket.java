package dev.slarrties.privit.common.network.payload.s2c;

import dev.slarrties.privit.PrivitMod;
import dev.slarrties.privit.common.network.payload.PrivitPacket;

import net.minecraft.util.Identifier;
import net.minecraft.network.PacketByteBuf;

public record RegionGridClearS2CPacket() implements PrivitPacket {
    public static final Identifier ID = PrivitMod.id("region_grid_clear_s2c");

    @Override
    public void write(PacketByteBuf buf) {}

    public static RegionGridClearS2CPacket read(PacketByteBuf buf) {
        return new RegionGridClearS2CPacket();
    }

    @Override
    public Identifier getId() {
        return ID;
    }
}