package dev.slarrties.privit.common.network.payload.c2s;

import dev.slarrties.privit.PrivitMod;

import net.minecraft.util.math.BlockPos;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record RegionGuiRequestC2SPacket(BlockPos tablePos) implements CustomPayload {

    public static final Id<RegionGuiRequestC2SPacket> ID = new Id<>(PrivitMod.id("region_gui_request"));
    public static final PacketCodec<PacketByteBuf, RegionGuiRequestC2SPacket> CODEC = PacketCodec.of(
            (value, buf) -> buf.writeBlockPos(value.tablePos),
            buf -> new RegionGuiRequestC2SPacket(buf.readBlockPos())
    );

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}