package dev.slarrties.privit.server.region.protection.handler.eat_chorus_fruit;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;
import dev.slarrties.privit.server.region.protection.handler.RuleEventHandler;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;

import net.minecraft.server.level.ServerPlayer;
import net.fabricmc.fabric.api.event.player.UseItemCallback;

@AssociatedRule(Rule.EAT_CHORUS_FRUITS)
public class EatChorusFruitHandler implements RuleEventHandler {

    @Override
    public Rule getRule() {
        return Rule.EAT_CHORUS_FRUITS;
    }

    public void register() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (world.isClientSide() || !(player instanceof ServerPlayer serverPlayer))
                return InteractionResult.PASS;

            ItemStack stack = player.getItemInHand(hand);
            if (!stack.is(Items.CHORUS_FRUIT)) return InteractionResult.PASS;

            boolean allowed = RegionPermissionChecker.isAllowed(serverPlayer, Rule.EAT_CHORUS_FRUITS, player.blockPosition());
            if (allowed) return InteractionResult.PASS;

            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_EAT_CHORUS_FRUIT, Color.RED);
            return InteractionResult.FAIL;
        });
    }
}