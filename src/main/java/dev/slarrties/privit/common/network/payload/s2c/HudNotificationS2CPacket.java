package dev.slarrties.privit.common.network.payload.s2c;

import dev.slarrties.privit.PrivitMod;
import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.notification.NotificationType;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record HudNotificationS2CPacket(NotificationType notificationType, Color color) implements CustomPacketPayload {
    public static final Type<HudNotificationS2CPacket> TYPE = new Type<>(PrivitMod.id("hud_notification"));

    public static final StreamCodec<FriendlyByteBuf, HudNotificationS2CPacket> CODEC = StreamCodec.of(
            (FriendlyByteBuf buf, HudNotificationS2CPacket packet) -> {
                buf.writeEnum(packet.notificationType);
                buf.writeUtf(packet.color.getCode());
            },
            buf -> new HudNotificationS2CPacket(
                    buf.readEnum(NotificationType.class),
                    Color.fromCode(buf.readUtf())
            )
    );

    public HudNotificationS2CPacket(NotificationType notificationType) {
        this(notificationType, Color.WHITE);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}