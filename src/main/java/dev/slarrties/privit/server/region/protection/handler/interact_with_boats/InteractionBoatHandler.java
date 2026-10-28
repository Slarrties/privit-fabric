package dev.slarrties.privit.server.region.protection.handler.interact_with_boats;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;
import dev.slarrties.privit.server.region.protection.handler.RuleEventHandler;
import dev.slarrties.privit.server.tracking.protection.InfluencedEntityTracker;

import net.minecraft.world.item.Items;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;

@AssociatedRule(Rule.INTERACT_WITH_BOATS)
public final class InteractionBoatHandler implements RuleEventHandler {

    @Override
    public Rule getRule() {
        return Rule.INTERACT_WITH_BOATS;
    }

    @Override
    public void register() {
        UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            if (!(player instanceof ServerPlayer serverPlayer) || level.isClientSide()) return InteractionResult.PASS;
            if (!(entity instanceof Boat boat)) return InteractionResult.PASS;

            boolean allowed = RegionPermissionChecker.isAllowed(serverPlayer, Rule.INTERACT_WITH_BOATS, boat.blockPosition());
            if (!allowed) {
                PlayerNotification.trySend(serverPlayer, NotificationType.DENY_INTERACT_BOAT, Color.RED);
                return InteractionResult.FAIL;
            }

            if (player.getItemInHand(hand).is(Items.LEAD)) {
                InfluencedEntityTracker entityTracker = WorldRegistry.get((ServerLevel) level)
                        .getTrackerManager()
                        .getInfluencedEntityTracker();
                for (var passenger : boat.getPassengers()) {
                    entityTracker.record(passenger, serverPlayer.getUUID());
                }
            }
            return InteractionResult.PASS;
        });

        AttackEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;
            if (!(entity instanceof Boat)) return InteractionResult.PASS;

            boolean allowed = RegionPermissionChecker.isAllowed(serverPlayer, Rule.INTERACT_WITH_BOATS, entity.blockPosition());
            if (allowed) return InteractionResult.PASS;

            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_INTERACT_BOAT, Color.RED);
            return InteractionResult.FAIL;
        });
    }
}