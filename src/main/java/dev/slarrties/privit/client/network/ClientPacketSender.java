package dev.slarrties.privit.client.network;

import dev.slarrties.privit.common.network.payload.PrivitPacket;

import net.minecraft.network.PacketByteBuf;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class ClientPacketSender {

    private ClientPacketSender() {}

    public static void send(PrivitPacket packet) {
        PacketByteBuf buf = PacketByteBufs.create();
        packet.write(buf);
        ClientPlayNetworking.send(packet.getId(), buf);
    }
}