package dev.slarrties.privit.server.region.protection.handler.throw_snowballs;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.util.InventorySyncSystem;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.handler.RuleEventHandler;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.SnowballItem;
import net.minecraft.server.level.ServerPlayer;
import net.fabricmc.fabric.api.event.player.UseItemCallback;

@AssociatedRule(Rule.THROW_SNOWBALLS)
public final class ThrowSnowballHandler implements RuleEventHandler {

    @Override
    public Rule getRule() { return Rule.THROW_SNOWBALLS; }

    @Override
    public void register() {
        UseItemCallback.EVENT.register((player, level, hand) -> {
            if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;
            if (!(player.getItemInHand(hand).getItem() instanceof SnowballItem)) return InteractionResult.PASS;

            BlockPos pos = player.blockPosition();
            boolean allowed = RegionPermissionChecker.isAllowed(serverPlayer, Rule.THROW_SNOWBALLS, pos);
            if (allowed) return InteractionResult.PASS;

            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_THROW_SNOWBALL, Color.RED);
            InventorySyncSystem.syncHandSlot(serverPlayer, hand);
            return InteractionResult.FAIL;
        });
    }
}