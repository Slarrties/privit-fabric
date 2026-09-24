package dev.slarrties.privit.common.network.payload.c2s;

import dev.slarrties.privit.PrivitMod;
import dev.slarrties.privit.common.network.payload.PrivitPacket;

import net.minecraft.util.Identifier;
import net.minecraft.network.PacketByteBuf;

import java.util.UUID;

public record RegionGuiCancelC2SPacket(UUID regionId) implements PrivitPacket {
    public static final Identifier ID = PrivitMod.id("region_gui_cancel_c2s");

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeUuid(regionId);
    }

    public static RegionGuiCancelC2SPacket read(PacketByteBuf buf) {
        return new RegionGuiCancelC2SPacket(buf.readUuid());
    }

    @Override
    public Identifier getId() {
        return ID;
    }
}