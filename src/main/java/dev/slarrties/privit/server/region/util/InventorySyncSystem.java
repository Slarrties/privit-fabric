package dev.slarrties.privit.server.region.util;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;

public final class InventorySyncSystem {

    private InventorySyncSystem() {}

    public static void syncHandSlot(ServerPlayer player, InteractionHand hand) {
        if (player == null) return;

        ItemStack stackInHand = player.getItemInHand(hand);
        int slotId = handSlotId(player, hand);
        player.connection.send(new ClientboundContainerSetSlotPacket(
                player.containerMenu.containerId,
                0,
                slotId,
                stackInHand.copy()
        ));
    }

    public static void syncHandSlotStrong(ServerPlayer player, InteractionHand hand) {
        if (player == null) return;

        ItemStack stackInHand = player.getItemInHand(hand).copy();
        int slotId = handSlotId(player, hand);
        player.connection.send(new ClientboundContainerSetSlotPacket(
                player.containerMenu.containerId,
                0,
                slotId,
                stackInHand
        ));
        player.containerMenu.broadcastChanges();
        player.connection.send(new ClientboundContainerSetContentPacket(
                player.containerMenu.containerId,
                0,
                player.containerMenu.getItems(),
                player.containerMenu.getCarried()
        ));
        player.getInventory().setChanged();
    }

    public static void syncFullInventory(ServerPlayer player) {
        if (player == null) return;

        player.containerMenu.broadcastChanges();
        player.getInventory().setChanged();
    }

    private static int handSlotId(ServerPlayer player, InteractionHand hand) {
        return (hand == InteractionHand.MAIN_HAND)
                ? player.getInventory().getSelectedSlot() + 36 // TODO: it may work incorrect
                : 45;
    }
}