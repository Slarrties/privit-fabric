package dev.slarrties.privit.server.region.protection.handler.throw_wind_charges;

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
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.TypedActionResult;
import net.minecraft.server.network.ServerPlayerEntity;
import net.fabricmc.fabric.api.event.player.UseItemCallback;

@AssociatedRule(Rule.THROW_WIND_CHARGES)
public final class ThrowWindChargeHandler implements RuleEventHandler {

    @Override
    public Rule getRule() { return Rule.THROW_WIND_CHARGES; }

    public void register() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (world.isClient || !(player instanceof ServerPlayerEntity serverPlayer))
                return TypedActionResult.pass(player.getStackInHand(hand));

            ItemStack stack = player.getStackInHand(hand);
            BlockPos pos = player.getBlockPos();
            boolean allowed = RegionPermissionChecker.isAllowed(serverPlayer, Rule.THROW_WIND_CHARGES, pos);
            if (!stack.isOf(Items.WIND_CHARGE)) return TypedActionResult.pass(stack);
            if (allowed) return TypedActionResult.pass(stack);

            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_THROW_WIND_CHARGE, Color.RED);
            InventorySyncSystem.syncHandSlot(serverPlayer, hand);

            return TypedActionResult.fail(stack);
        });
    }
}