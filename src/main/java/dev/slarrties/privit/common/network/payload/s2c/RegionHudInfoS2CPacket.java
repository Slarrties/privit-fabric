package dev.slarrties.privit.common.network.payload.s2c;

import dev.slarrties.privit.PrivitMod;
import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.network.payload.PrivitPacket;

import net.minecraft.util.Identifier;
import net.minecraft.network.PacketByteBuf;

import java.util.Optional;

public record RegionHudInfoS2CPacket(Optional<String> regionName, Optional<Color> color) implements PrivitPacket {
    public static final Identifier ID = PrivitMod.id("region_hud_info");

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeBoolean(regionName.isPresent());
        regionName.ifPresent(buf::writeString);
        buf.writeBoolean(color.isPresent());
        color.ifPresent(c -> buf.writeString(c.getCode()));
    }

    public static RegionHudInfoS2CPacket read(PacketByteBuf buf) {
        return new RegionHudInfoS2CPacket(
                buf.readBoolean() ? Optional.of(buf.readString()) : Optional.empty(),
                buf.readBoolean() ? Optional.of(Color.fromCode(buf.readString())) : Optional.empty()
        );
    }

    @Override
    public Identifier getId() {
        return ID;
    }
}