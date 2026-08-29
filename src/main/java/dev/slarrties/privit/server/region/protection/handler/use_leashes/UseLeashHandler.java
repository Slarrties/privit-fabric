package dev.slarrties.privit.server.region.protection.handler.use_leashes;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.util.InventorySyncSystem;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.handler.RuleEventHandler;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;

import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.entity.Leashable;
import net.minecraft.server.network.ServerPlayerEntity;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;

@AssociatedRule(Rule.USE_LEASHES)
public final class UseLeashHandler implements RuleEventHandler {

    @Override
    public Rule getRule() { return Rule.USE_LEASHES; }

    @Override
    public void register() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient) return ActionResult.PASS;
            if (!(player instanceof ServerPlayerEntity serverPlayer)) return ActionResult.PASS;
            if (serverPlayer.getStackInHand(hand).getItem() != Items.LEAD) return ActionResult.PASS;
            if (!(entity instanceof Leashable leashable) || !leashable.canBeLeashed()) return ActionResult.PASS;

            boolean allowed = RegionPermissionChecker.isAllowed(serverPlayer, Rule.USE_LEASHES, entity.getBlockPos());
            if (allowed) return ActionResult.PASS;

            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_USE_LEASH, Color.RED);
            InventorySyncSystem.syncHandSlot(serverPlayer, hand);

            return ActionResult.FAIL;
        });
    }
}