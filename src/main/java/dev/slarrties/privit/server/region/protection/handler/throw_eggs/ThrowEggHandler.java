package dev.slarrties.privit.server.region.protection.handler.throw_eggs;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.util.InventorySyncSystem;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.handler.RuleEventHandler;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;

import net.minecraft.item.EggItem;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.TypedActionResult;
import net.minecraft.server.network.ServerPlayerEntity;
import net.fabricmc.fabric.api.event.player.UseItemCallback;

@AssociatedRule(Rule.THROW_EGGS)
public final class ThrowEggHandler implements RuleEventHandler {

    @Override
    public Rule getRule() { return Rule.THROW_EGGS; }

    @Override
    public void register() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (world.isClient || !(player instanceof ServerPlayerEntity serverPlayer))
                return TypedActionResult.pass(player.getStackInHand(hand));

            if (!(player.getStackInHand(hand).getItem() instanceof EggItem))
                return TypedActionResult.pass(player.getStackInHand(hand));

            BlockPos pos = player.getBlockPos();
            boolean allowed = RegionPermissionChecker.isAllowed(serverPlayer, Rule.THROW_EGGS, pos);
            if (allowed) return TypedActionResult.pass(player.getStackInHand(hand));

            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_THROW_EGG, Color.RED);
            InventorySyncSystem.syncHandSlot(serverPlayer, hand);

            return TypedActionResult.fail(player.getStackInHand(hand));
        });
    }
}