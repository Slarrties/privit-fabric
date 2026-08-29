package dev.slarrties.privit.server.region.util;

import net.minecraft.util.Hand;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;

public final class InventorySyncSystem {

    private InventorySyncSystem() {}

    public static void syncHandSlot(ServerPlayerEntity player, Hand hand) {
        if (player == null || player.networkHandler == null) return;

        ItemStack stackInHand = player.getStackInHand(hand);
        int slotId = handSlotId(player, hand);

        player.networkHandler.sendPacket(new ScreenHandlerSlotUpdateS2CPacket(
                player.currentScreenHandler.syncId,
                0,
                slotId,
                stackInHand.copy()
        ));
    }

    public static void syncHandSlotStrong(ServerPlayerEntity player, Hand hand) {
        if (player == null || player.networkHandler == null) return;

        ItemStack stackInHand = player.getStackInHand(hand).copy();
        int slotId = handSlotId(player, hand);

        player.networkHandler.sendPacket(new ScreenHandlerSlotUpdateS2CPacket(
                player.currentScreenHandler.syncId,
                0,
                slotId,
                stackInHand
        ));

        player.currentScreenHandler.sendContentUpdates();

        player.networkHandler.sendPacket(new InventoryS2CPacket(
                player.currentScreenHandler.syncId,
                0,
                player.currentScreenHandler.getStacks(),
                player.currentScreenHandler.getCursorStack()
        ));

        player.getInventory().markDirty();
    }

    public static void syncFullInventory(ServerPlayerEntity player) {
        if (player == null || player.networkHandler == null) return;
        player.currentScreenHandler.syncState();
        player.getInventory().markDirty();
    }

    private static int handSlotId(ServerPlayerEntity player, Hand hand) {
        // Hotbar: 36–44, Offhand: 45
        return (hand == Hand.MAIN_HAND)
                ? player.getInventory().selectedSlot + 36
                : 45;
    }
}