package dev.slarrties.privit.common.network.payload.s2c;

import dev.slarrties.privit.PrivitMod;
import dev.slarrties.privit.common.region.Color;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.Optional;

public record RegionHudInfoS2CPacket(Optional<String> regionName, Optional<Color> color) implements CustomPacketPayload {
    public static final Type<RegionHudInfoS2CPacket> TYPE = new Type<>(PrivitMod.id("region_hud_info"));

    public static final StreamCodec<FriendlyByteBuf, RegionHudInfoS2CPacket> CODEC = StreamCodec.of(
            (FriendlyByteBuf buf, RegionHudInfoS2CPacket packet) -> {
                buf.writeBoolean(packet.regionName.isPresent());
                packet.regionName.ifPresent(name -> buf.writeUtf(name));
                buf.writeBoolean(packet.color.isPresent());
                packet.color.ifPresent(c -> buf.writeUtf(c.getCode()));
            },
            buf -> new RegionHudInfoS2CPacket(
                    buf.readBoolean() ? Optional.of(buf.readUtf()) : Optional.empty(),
                    buf.readBoolean() ? Optional.of(Color.fromCode(buf.readUtf())) : Optional.empty()
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}