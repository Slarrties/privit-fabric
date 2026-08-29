package dev.slarrties.privit.server.region.protection.handler.trade_with_villagers;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.handler.RuleEventHandler;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;

import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.passive.WanderingTraderEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;

@AssociatedRule(Rule.TRADE_WITH_VILLAGERS)
public final class TradeVillagerHandler implements RuleEventHandler {

    @Override
    public Rule getRule() { return Rule.TRADE_WITH_VILLAGERS; }

    @Override
    public void register() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!(player instanceof ServerPlayerEntity serverPlayer) || world.isClient) return ActionResult.PASS;
            if (hand != Hand.MAIN_HAND) return ActionResult.PASS;
            if (!(entity instanceof VillagerEntity) && !(entity instanceof WanderingTraderEntity)) return ActionResult.PASS;

            BlockPos pos = entity.getBlockPos();
            boolean allowed = RegionPermissionChecker.isAllowed(serverPlayer, Rule.TRADE_WITH_VILLAGERS, pos);
            if (allowed) return ActionResult.PASS;

            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_TRADE_VILLAGER, Color.RED);

            return ActionResult.FAIL;
        });
    }
}