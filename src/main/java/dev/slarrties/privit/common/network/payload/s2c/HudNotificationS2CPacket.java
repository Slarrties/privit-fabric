package dev.slarrties.privit.common.network.payload.s2c;

import dev.slarrties.privit.PrivitMod;
import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.notification.NotificationType;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record HudNotificationS2CPacket(NotificationType type, Color color) implements CustomPayload {

    public static final Id<HudNotificationS2CPacket> ID = new Id<>(PrivitMod.id("hud_notification"));

    public static final PacketCodec<RegistryByteBuf, HudNotificationS2CPacket> CODEC = PacketCodec.of(
            (packet, buf) -> {
                buf.writeEnumConstant(packet.type);
                buf.writeString(packet.color.getCode());
            },
            buf -> new HudNotificationS2CPacket(
                    buf.readEnumConstant(NotificationType.class),
                    Color.fromCode(buf.readString())
            )
    );

    public HudNotificationS2CPacket(NotificationType type) { this(type, Color.WHITE); }

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}