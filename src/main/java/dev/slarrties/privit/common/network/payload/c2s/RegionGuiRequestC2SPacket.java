package dev.slarrties.privit.common.network.payload.c2s;

import dev.slarrties.privit.PrivitMod;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record RegionGuiRequestC2SPacket(BlockPos tablePos) implements CustomPacketPayload {
    public static final Type<RegionGuiRequestC2SPacket> TYPE = new Type<>(PrivitMod.id("region_gui_request"));
    public static final StreamCodec<FriendlyByteBuf, RegionGuiRequestC2SPacket> CODEC = StreamCodec.of(
            (FriendlyByteBuf buf, RegionGuiRequestC2SPacket packet) -> buf.writeBlockPos(packet.tablePos),
            buf -> new RegionGuiRequestC2SPacket(buf.readBlockPos())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}