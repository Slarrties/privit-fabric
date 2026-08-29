package dev.slarrties.privit.server.region.protection.handler.interact_with_boats;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.handler.RuleEventHandler;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;
import dev.slarrties.privit.server.tracking.protection.InfluencedEntityTracker;

import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;
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
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!(player instanceof ServerPlayerEntity serverPlayer) || world.isClient) return ActionResult.PASS;
            if (!(entity instanceof BoatEntity boat)) return ActionResult.PASS;

            boolean allowed = RegionPermissionChecker.isAllowed(serverPlayer, Rule.INTERACT_WITH_BOATS, boat.getBlockPos());

            if (!allowed) {
                PlayerNotification.trySend(serverPlayer, NotificationType.DENY_INTERACT_BOAT, Color.RED);
                return ActionResult.FAIL;
            }

            if (player.getStackInHand(hand).isOf(Items.LEAD)) {
                InfluencedEntityTracker entityTracker = WorldRegistry.get((ServerWorld) world)
                        .getTrackerManager()
                        .getInfluencedEntityTracker();

                for (var passenger : boat.getPassengerList()) {
                    entityTracker.record(passenger, serverPlayer.getUuid());
                }
            }

            return ActionResult.PASS;
        });

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!(player instanceof ServerPlayerEntity serverPlayer)) return ActionResult.PASS;
            if (!(entity instanceof BoatEntity)) return ActionResult.PASS;

            boolean allowed = RegionPermissionChecker.isAllowed(serverPlayer, Rule.INTERACT_WITH_BOATS, entity.getBlockPos());
            if (allowed) return ActionResult.PASS;

            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_INTERACT_BOAT, Color.RED);

            return ActionResult.FAIL;
        });
    }
}