package dev.slarrties.privit.server.region.protection.handler.use_fishing_rods;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.PlayerPermissionCache;
import dev.slarrties.privit.server.region.protection.handler.RuleEventHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionResult;
import net.minecraft.server.level.ServerPlayer;
import net.fabricmc.fabric.api.event.player.UseItemCallback;

@AssociatedRule(Rule.USE_FISHING_RODS)
public final class UseFishingRodHandler implements RuleEventHandler {

    @Override
    public Rule getRule() { return Rule.USE_FISHING_RODS; }

    @Override
    public void register() {
        UseItemCallback.EVENT.register((player, level, hand) -> {
            if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;

            ItemStack stack = player.getItemInHand(hand);
            if (!stack.is(Items.FISHING_ROD)) return InteractionResult.PASS;

            BlockPos pos = player.blockPosition();
            boolean allowed = PlayerPermissionCache.isAllowed(serverPlayer, Rule.USE_FISHING_RODS, pos);
            if (allowed) return InteractionResult.PASS;

            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_USE_FISHING_ROD, Color.RED);
            return InteractionResult.FAIL;
        });
    }
}