package dev.slarrties.privit.common.network.payload.c2s;

import dev.slarrties.privit.PrivitMod;
import dev.slarrties.privit.common.network.payload.PrivitPacket;

import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.network.PacketByteBuf;

public record RegionGuiRequestC2SPacket(BlockPos tablePos) implements PrivitPacket {
    public static final Identifier ID = PrivitMod.id("region_gui_request");

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeBlockPos(tablePos);
    }

    public static RegionGuiRequestC2SPacket read(PacketByteBuf buf) {
        return new RegionGuiRequestC2SPacket(buf.readBlockPos());
    }

    @Override
    public Identifier getId() {
        return ID;
    }
}