package dev.slarrties.privit.common.network.payload.s2c;

import dev.slarrties.privit.PrivitMod;
import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.network.payload.PrivitPacket;
import dev.slarrties.privit.common.notification.NotificationType;

import net.minecraft.util.Identifier;
import net.minecraft.network.PacketByteBuf;

public record HudNotificationS2CPacket(NotificationType type, Color color) implements PrivitPacket {
    public static final Identifier ID = PrivitMod.id("hud_notification");

    public HudNotificationS2CPacket(NotificationType type) {
        this(type, Color.WHITE);
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeEnumConstant(type);
        buf.writeString(color.getCode());
    }

    public static HudNotificationS2CPacket read(PacketByteBuf buf) {
        return new HudNotificationS2CPacket(
                buf.readEnumConstant(NotificationType.class),
                Color.fromCode(buf.readString())
        );
    }

    @Override
    public Identifier getId() {
        return ID;
    }
}