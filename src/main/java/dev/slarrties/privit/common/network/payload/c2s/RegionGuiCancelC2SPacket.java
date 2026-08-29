package dev.slarrties.privit.common.network.payload.c2s;

import dev.slarrties.privit.PrivitMod;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

import java.util.UUID;

public record RegionGuiCancelC2SPacket(UUID regionId) implements CustomPayload {

    public static final Id<RegionGuiCancelC2SPacket> ID = new Id<>(PrivitMod.id("region_gui_cancel_c2s"));
    public static final PacketCodec<PacketByteBuf, RegionGuiCancelC2SPacket> CODEC =
            PacketCodec.of(RegionGuiCancelC2SPacket::write, RegionGuiCancelC2SPacket::read);

    private void write(PacketByteBuf buf) { buf.writeUuid(regionId); }

    private static RegionGuiCancelC2SPacket read(PacketByteBuf buf) {
        return new RegionGuiCancelC2SPacket(buf.readUuid());
    }

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}