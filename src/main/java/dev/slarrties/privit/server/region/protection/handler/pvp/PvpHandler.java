package dev.slarrties.privit.server.region.protection.handler.pvp;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.handler.RuleEventHandler;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.server.level.ServerPlayer;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;

@AssociatedRule(Rule.PVP)
public final class PvpHandler implements RuleEventHandler {

    @Override
    public Rule getRule() { return Rule.PVP; }

    @Override
    public void register() {

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;
            if (!(entity instanceof ServerPlayer victim)) return InteractionResult.PASS;

            BlockPos pos = victim.getOnPos();
            boolean attackerAllowed = RegionPermissionChecker.isAllowed(serverPlayer, Rule.PVP, pos);
            boolean victimAllowed = RegionPermissionChecker.isAllowed(victim, Rule.PVP, pos);
            if (attackerAllowed && victimAllowed) return InteractionResult.PASS;

            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_PVP, Color.RED);

            return InteractionResult.FAIL;
        });
    }
}