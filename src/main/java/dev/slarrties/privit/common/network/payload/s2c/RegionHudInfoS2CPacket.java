package dev.slarrties.privit.common.network.payload.s2c;

import dev.slarrties.privit.PrivitMod;
import dev.slarrties.privit.common.region.Color;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

import java.util.Optional;

public record RegionHudInfoS2CPacket(Optional<String> regionName, Optional<Color> color) implements CustomPayload {

    public static final Id<RegionHudInfoS2CPacket> ID = new Id<>(PrivitMod.id("region_hud_info"));

    public static final PacketCodec<PacketByteBuf, RegionHudInfoS2CPacket> CODEC = PacketCodec.of(
            (value, buf) -> {
                buf.writeBoolean(value.regionName.isPresent());
                value.regionName.ifPresent(buf::writeString);

                buf.writeBoolean(value.color.isPresent());
                value.color.ifPresent(c -> buf.writeString(c.getCode()));
            },
            buf -> new RegionHudInfoS2CPacket(
                    buf.readBoolean() ? Optional.of(buf.readString()) : Optional.empty(),
                    buf.readBoolean() ? Optional.of(Color.fromCode(buf.readString())) : Optional.empty()
            )
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}