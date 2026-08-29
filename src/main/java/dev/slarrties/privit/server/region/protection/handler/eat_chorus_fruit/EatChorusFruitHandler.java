package dev.slarrties.privit.server.region.protection.handler.eat_chorus_fruit;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;
import dev.slarrties.privit.server.region.protection.handler.RuleEventHandler;

import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.TypedActionResult;
import net.minecraft.server.network.ServerPlayerEntity;
import net.fabricmc.fabric.api.event.player.UseItemCallback;

@AssociatedRule(Rule.EAT_CHORUS_FRUITS)
public class EatChorusFruitHandler implements RuleEventHandler {

    @Override
    public Rule getRule() { return Rule.EAT_CHORUS_FRUITS; }

    public void register() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (world.isClient || !(player instanceof ServerPlayerEntity serverPlayer))
                return TypedActionResult.pass(player.getStackInHand(hand));

            ItemStack stack = player.getStackInHand(hand);
            if (!stack.isOf(Items.CHORUS_FRUIT)) return TypedActionResult.pass(stack);

            boolean allowed = RegionPermissionChecker.isAllowed(serverPlayer, Rule.EAT_CHORUS_FRUITS, player.getBlockPos());
            if (allowed) return TypedActionResult.pass(stack);

            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_EAT_CHORUS_FRUIT, Color.RED);

            return TypedActionResult.fail(stack);
        });
    }
}