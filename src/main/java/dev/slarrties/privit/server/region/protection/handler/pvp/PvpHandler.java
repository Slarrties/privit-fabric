package dev.slarrties.privit.server.region.protection.handler.pvp;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.handler.RuleEventHandler;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;

import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.server.network.ServerPlayerEntity;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;

@AssociatedRule(Rule.PVP)
public final class PvpHandler implements RuleEventHandler {

    @Override
    public Rule getRule() { return Rule.PVP; }

    @Override
    public void register() {

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient || !(player instanceof ServerPlayerEntity serverPlayer)) return ActionResult.PASS;
            if (!(entity instanceof ServerPlayerEntity victim)) return ActionResult.PASS;

            BlockPos pos = victim.getBlockPos();
            boolean attackerAllowed = RegionPermissionChecker.isAllowed(serverPlayer, Rule.PVP, pos);
            boolean victimAllowed   = RegionPermissionChecker.isAllowed(victim,   Rule.PVP, pos);
            if (attackerAllowed && victimAllowed) return ActionResult.PASS;

            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_PVP, Color.RED);

            return ActionResult.FAIL;
        });
    }
}