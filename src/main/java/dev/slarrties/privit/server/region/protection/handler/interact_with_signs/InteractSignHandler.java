package dev.slarrties.privit.server.region.protection.handler.interact_with_signs;

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
import net.minecraft.block.SignBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;

@AssociatedRule(Rule.INTERACT_WITH_SIGNS)
public final class InteractSignHandler implements RuleEventHandler {

    @Override
    public Rule getRule() { return Rule.INTERACT_WITH_SIGNS; }

    @Override
    public void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient || !(player instanceof ServerPlayerEntity serverPlayer)) return ActionResult.PASS;

            BlockPos pos = hitResult.getBlockPos();
            BlockState state = world.getBlockState(pos);
            if (!(state.getBlock() instanceof SignBlock) && !(world.getBlockEntity(pos) instanceof SignBlockEntity))
                return ActionResult.PASS;

            boolean allowed = RegionPermissionChecker.isAllowed(serverPlayer, Rule.INTERACT_WITH_SIGNS, pos);
            if (allowed) return ActionResult.PASS;

            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_INTERACT_SIGN, Color.RED);
            InventorySyncSystem.syncHandSlot(serverPlayer, hand);

            return ActionResult.FAIL;
        });
    }
}