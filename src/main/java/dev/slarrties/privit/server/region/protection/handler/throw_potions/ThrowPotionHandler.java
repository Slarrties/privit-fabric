package dev.slarrties.privit.server.region.protection.handler.throw_potions;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.util.InventorySyncSystem;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.handler.RuleEventHandler;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;

import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.TypedActionResult;
import net.minecraft.server.network.ServerPlayerEntity;
import net.fabricmc.fabric.api.event.player.UseItemCallback;

@AssociatedRule(Rule.THROW_POTIONS)
public class ThrowPotionHandler implements RuleEventHandler {

    @Override
    public Rule getRule() {
        return Rule.THROW_POTIONS;
    }

    @Override
    public void register() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (world.isClient || !(player instanceof ServerPlayerEntity serverPlayer))
                return TypedActionResult.pass(player.getStackInHand(hand));

            ItemStack stack = player.getStackInHand(hand);
            if (!stack.isOf(Items.SPLASH_POTION) && !stack.isOf(Items.LINGERING_POTION))
                return TypedActionResult.pass(stack);

            boolean allowed = RegionPermissionChecker.isAllowed(serverPlayer, Rule.THROW_POTIONS, player.getBlockPos());
            if (allowed) return TypedActionResult.pass(stack);

            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_THROW_POTION, Color.RED);
            InventorySyncSystem.syncHandSlot(serverPlayer, hand);

            return TypedActionResult.fail(stack);
        });
    }
}