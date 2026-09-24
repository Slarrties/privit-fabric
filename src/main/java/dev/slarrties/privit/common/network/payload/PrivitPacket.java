package dev.slarrties.privit.common.network.payload;

import net.minecraft.util.Identifier;
import net.minecraft.network.PacketByteBuf;

public interface PrivitPacket {
    Identifier getId();
    void write(PacketByteBuf buf);
}