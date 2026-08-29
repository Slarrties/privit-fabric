package dev.slarrties.privit.common.network.payload.s2c;

import dev.slarrties.privit.PrivitMod;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

import java.util.UUID;

public record RegionGuiCloseS2CPacket(UUID regionId) implements CustomPayload {

    public static final Id<RegionGuiCloseS2CPacket> ID = new Id<>(PrivitMod.id("region_gui_close_s2c"));
    public static final PacketCodec<PacketByteBuf, RegionGuiCloseS2CPacket> CODEC =
            PacketCodec.of(RegionGuiCloseS2CPacket::write, RegionGuiCloseS2CPacket::read);

    private void write(PacketByteBuf buf) {
        buf.writeUuid(regionId);
    }

    private static RegionGuiCloseS2CPacket read(PacketByteBuf buf) {
        return new RegionGuiCloseS2CPacket(buf.readUuid());
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}