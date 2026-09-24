package dev.slarrties.privit.common.network.payload.s2c;

import dev.slarrties.privit.PrivitMod;
import dev.slarrties.privit.common.network.payload.PrivitPacket;

import net.minecraft.util.Identifier;
import net.minecraft.network.PacketByteBuf;

import java.util.UUID;

public record RegionGuiCloseS2CPacket(UUID regionId) implements PrivitPacket {
    public static final Identifier ID = PrivitMod.id("region_gui_close_s2c");

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeUuid(regionId);
    }

    public static RegionGuiCloseS2CPacket read(PacketByteBuf buf) {
        return new RegionGuiCloseS2CPacket(buf.readUuid());
    }

    @Override
    public Identifier getId() {
        return ID;
    }
}