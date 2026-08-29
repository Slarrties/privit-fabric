package dev.slarrties.privit.server.region.protection.handler.attack_passive_mobs;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.handler.RuleEventHandler;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;

import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.*;
import net.minecraft.util.ActionResult;
import net.minecraft.server.network.ServerPlayerEntity;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;

@AssociatedRule(Rule.ATTACK_PASSIVE_MOBS)
public final class AttackPassiveHandler implements RuleEventHandler {

    @Override
    public Rule getRule() {
        return Rule.ATTACK_PASSIVE_MOBS;
    }

    @Override
    public void register() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!(player instanceof ServerPlayerEntity serverPlayer)) return ActionResult.PASS;
            if (!isPassive(entity)) return ActionResult.PASS;

            boolean allowed = RegionPermissionChecker.isAllowed(serverPlayer, Rule.ATTACK_PASSIVE_MOBS, entity.getBlockPos());
            if (allowed) return ActionResult.PASS;

            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_ATTACK_PASSIVE_MOB, Color.RED);

            return ActionResult.FAIL;
        });
    }

    // TODO: check the list of mobs more closely
    private static boolean isPassive(Entity entity) {
        return entity instanceof PassiveEntity ||
                entity instanceof BatEntity ||
                entity instanceof AllayEntity ||
                entity instanceof AxolotlEntity ||
                entity instanceof WanderingTraderEntity;
    }
}