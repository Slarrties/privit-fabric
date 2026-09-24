package dev.slarrties.privit.server.network;

import dev.slarrties.privit.common.network.payload.PrivitPacket;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class ServerPacketSender {

    private ServerPacketSender() {}

    public static void send(ServerPlayerEntity player, PrivitPacket packet) {
        PacketByteBuf buf = PacketByteBufs.create();
        packet.write(buf);
        ServerPlayNetworking.send(player, packet.getId(), buf);
    }
}