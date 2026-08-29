package dev.slarrties.privit.server.region.protection.handler.use_fishing_rods;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.PlayerPermissionCache;
import dev.slarrties.privit.server.region.protection.handler.RuleEventHandler;

import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.TypedActionResult;
import net.minecraft.server.network.ServerPlayerEntity;
import net.fabricmc.fabric.api.event.player.UseItemCallback;

@AssociatedRule(Rule.USE_FISHING_RODS)
public final class UseFishingRodHandler implements RuleEventHandler {

    @Override
    public Rule getRule() { return Rule.USE_FISHING_RODS; }

    public void register() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (world.isClient || !(player instanceof ServerPlayerEntity serverPlayer))
                return TypedActionResult.pass(player.getStackInHand(hand));

            ItemStack stack = player.getStackInHand(hand);
            if (!stack.isOf(Items.FISHING_ROD)) return TypedActionResult.pass(stack);

            BlockPos pos = player.getBlockPos();
            boolean allowed = PlayerPermissionCache.isAllowed(serverPlayer, Rule.USE_FISHING_RODS, pos);
            if (allowed) return TypedActionResult.pass(stack);

            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_USE_FISHING_ROD, Color.RED);

            return TypedActionResult.fail(stack);
        });
    }
}