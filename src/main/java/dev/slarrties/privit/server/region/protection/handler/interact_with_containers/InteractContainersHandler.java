package dev.slarrties.privit.server.region.protection.handler.interact_with_containers;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.util.InventorySyncSystem;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.handler.RuleEventHandler;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;

import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.inventory.Inventory;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;

@AssociatedRule(Rule.INTERACT_WITH_CONTAINERS)
public final class InteractContainersHandler implements RuleEventHandler {

    @Override
    public Rule getRule() {
        return Rule.INTERACT_WITH_CONTAINERS;
    }

    @Override
    public void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient || !(player instanceof ServerPlayerEntity serverPlayer)) return ActionResult.PASS;

            BlockPos pos = hitResult.getBlockPos();
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof EnderChestBlockEntity) return ActionResult.PASS;

            boolean isContainer = isContainer(blockEntity);
            if (!isContainer) return ActionResult.PASS;

            boolean allowed = RegionPermissionChecker.isAllowed(serverPlayer, Rule.INTERACT_WITH_CONTAINERS, pos);
            if (allowed) return ActionResult.PASS;

            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_INTERACT_CONTAINER, Color.RED);
            InventorySyncSystem.syncHandSlot(serverPlayer, hand);

            return ActionResult.FAIL;
        });
    }

    private static boolean isContainer(BlockEntity blockEntity) {
        if (blockEntity == null) return false;
        if (blockEntity instanceof Inventory) return true;

        String id = blockEntity.getType().toString().toLowerCase();
        return id.contains("chest") ||
                id.contains("dispenser") ||
                id.contains("dropper") ||
                id.contains("hopper") ||
                id.contains("furnace") ||
                id.contains("brewing") ||
                id.contains("shulker") ||
                id.contains("barrel");
    }
}