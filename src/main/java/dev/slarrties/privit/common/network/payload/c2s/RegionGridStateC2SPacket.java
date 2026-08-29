package dev.slarrties.privit.common.network.payload.c2s;

import dev.slarrties.privit.PrivitMod;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

import java.util.UUID;

public record RegionGridStateC2SPacket(UUID regionId, boolean enabled) implements CustomPayload {

    public static final Id<RegionGridStateC2SPacket> ID = new Id<>(PrivitMod.id("region_grid_state_c2s"));

    public static final PacketCodec<PacketByteBuf, RegionGridStateC2SPacket> CODEC = PacketCodec.of(
            (value, buf) -> {
                buf.writeUuid(value.regionId);
                buf.writeBoolean(value.enabled);
            },
            buf -> new RegionGridStateC2SPacket(
                    buf.readUuid(),
                    buf.readBoolean()
            )
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}