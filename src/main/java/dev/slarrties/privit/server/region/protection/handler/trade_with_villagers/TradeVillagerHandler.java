package dev.slarrties.privit.server.region.protection.handler.trade_with_villagers;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.handler.RuleEventHandler;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.server.level.ServerPlayer;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;

@AssociatedRule(Rule.TRADE_WITH_VILLAGERS)
public final class TradeVillagerHandler implements RuleEventHandler {
    @Override
    public Rule getRule() {
        return Rule.TRADE_WITH_VILLAGERS;
    }

    @Override
    public void register() {
        UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            if (!(player instanceof ServerPlayer serverPlayer) || level.isClientSide()) return InteractionResult.PASS;
            if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
            if (!(entity instanceof Villager) && !(entity instanceof WanderingTrader)) return InteractionResult.PASS;

            BlockPos pos = entity.blockPosition();
            boolean allowed = RegionPermissionChecker.isAllowed(serverPlayer, Rule.TRADE_WITH_VILLAGERS, pos);
            if (allowed) return InteractionResult.PASS;

            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_TRADE_VILLAGER, Color.RED);
            return InteractionResult.FAIL;
        });
    }
}