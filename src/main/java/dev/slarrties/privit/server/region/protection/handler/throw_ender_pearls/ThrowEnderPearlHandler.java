package dev.slarrties.privit.server.region.protection.handler.throw_ender_pearls;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.util.InventorySyncSystem;
import dev.slarrties.privit.server.region.protection.*;
import dev.slarrties.privit.server.region.protection.handler.RuleEventHandler;

import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.TypedActionResult;
import net.minecraft.server.network.ServerPlayerEntity;
import net.fabricmc.fabric.api.event.player.UseItemCallback;

@AssociatedRule(Rule.THROW_ENDER_PEARLS)
public class ThrowEnderPearlHandler implements RuleEventHandler {

    @Override
    public Rule getRule() { return Rule.THROW_ENDER_PEARLS; }

    public void register() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (world.isClient || !(player instanceof ServerPlayerEntity serverPlayer))
                return TypedActionResult.pass(player.getStackInHand(hand));

            ItemStack stack = player.getStackInHand(hand);
            if (!stack.isOf(Items.ENDER_PEARL)) return TypedActionResult.pass(stack);

            BlockPos pos = player.getBlockPos();
            boolean allowed = RegionPermissionChecker.isAllowed(serverPlayer, Rule.THROW_ENDER_PEARLS, pos);
            if (allowed) return TypedActionResult.pass(stack);

            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_THROW_ENDER_PEARL, Color.RED);
            InventorySyncSystem.syncHandSlot(serverPlayer, hand);

            return TypedActionResult.fail(stack);
        });
    }
}