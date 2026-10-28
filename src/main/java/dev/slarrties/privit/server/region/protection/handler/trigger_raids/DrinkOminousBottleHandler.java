package dev.slarrties.privit.server.region.protection.handler.trigger_raids;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.util.InventorySyncSystem;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.handler.RuleEventHandler;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionResult;
import net.minecraft.server.level.ServerPlayer;
import net.fabricmc.fabric.api.event.player.UseItemCallback;

@AssociatedRule(Rule.TRIGGER_RAIDS)
public final class DrinkOminousBottleHandler implements RuleEventHandler {

    @Override
    public Rule getRule() {
        return Rule.TRIGGER_RAIDS;
    }

    @Override
    public void register() {
        UseItemCallback.EVENT.register((player, level, hand) -> {
            if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;

            ItemStack stack = player.getItemInHand(hand);
            if (!stack.is(Items.OMINOUS_BOTTLE)) return InteractionResult.PASS;

            BlockPos pos = player.blockPosition();
            boolean allowed = RegionPermissionChecker.isAllowed(serverPlayer, Rule.TRIGGER_RAIDS, pos);
            if (allowed) return InteractionResult.PASS;

            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_TRIGGER_RAID, Color.RED);
            InventorySyncSystem.syncHandSlot(serverPlayer, hand);
            return InteractionResult.FAIL;
        });
    }
}