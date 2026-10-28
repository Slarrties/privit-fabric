package dev.slarrties.privit.server.region.protection.handler.interact_with_containers;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.util.InventorySyncSystem;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.handler.RuleEventHandler;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.server.level.ServerPlayer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;

@AssociatedRule(Rule.INTERACT_WITH_CONTAINERS)
public final class InteractContainersHandler implements RuleEventHandler {

    @Override
    public Rule getRule() {
        return Rule.INTERACT_WITH_CONTAINERS;
    }

    @Override
    public void register() {
        UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
            if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;
            BlockPos pos = hitResult.getBlockPos();
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof EnderChestBlockEntity) return InteractionResult.PASS;
            boolean isContainer = isContainer(blockEntity);
            if (!isContainer) return InteractionResult.PASS;
            boolean allowed = RegionPermissionChecker.isAllowed(serverPlayer, Rule.INTERACT_WITH_CONTAINERS, pos);
            if (allowed) return InteractionResult.PASS;
            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_INTERACT_CONTAINER, Color.RED);
            InventorySyncSystem.syncHandSlot(serverPlayer, hand);
            return InteractionResult.FAIL;
        });
    }

    private static boolean isContainer(BlockEntity blockEntity) {
        if (blockEntity == null) return false;
        if (blockEntity instanceof Container) return true;
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