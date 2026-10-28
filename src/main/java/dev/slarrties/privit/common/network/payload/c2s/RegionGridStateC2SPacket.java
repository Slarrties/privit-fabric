package dev.slarrties.privit.common.network.payload.c2s;

import dev.slarrties.privit.PrivitMod;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record RegionGridStateC2SPacket(UUID regionId, boolean enabled) implements CustomPacketPayload {
    public static final Type<RegionGridStateC2SPacket> TYPE = new Type<>(PrivitMod.id("region_grid_state_c2s"));
    public static final StreamCodec<FriendlyByteBuf, RegionGridStateC2SPacket> CODEC = StreamCodec.of(
            (FriendlyByteBuf buf, RegionGridStateC2SPacket packet) -> {
                buf.writeUUID(packet.regionId);
                buf.writeBoolean(packet.enabled);
            },
            buf -> new RegionGridStateC2SPacket(buf.readUUID(), buf.readBoolean())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}