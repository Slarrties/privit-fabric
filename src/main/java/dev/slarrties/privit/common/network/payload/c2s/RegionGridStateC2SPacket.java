package dev.slarrties.privit.common.network.payload.c2s;

import dev.slarrties.privit.PrivitMod;
import dev.slarrties.privit.common.network.payload.PrivitPacket;

import net.minecraft.util.Identifier;
import net.minecraft.network.PacketByteBuf;

import java.util.UUID;

public record RegionGridStateC2SPacket(UUID regionId, boolean enabled) implements PrivitPacket {
    public static final Identifier ID = PrivitMod.id("region_grid_state_c2s");

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeUuid(regionId);
        buf.writeBoolean(enabled);
    }

    public static RegionGridStateC2SPacket read(PacketByteBuf buf) {
        return new RegionGridStateC2SPacket(buf.readUuid(), buf.readBoolean());
    }

    @Override
    public Identifier getId() {
        return ID;
    }
}