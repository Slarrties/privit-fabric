package dev.slarrties.privit.server.region.protection.handler.attack_passive_mobs;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;
import dev.slarrties.privit.server.region.protection.handler.RuleEventHandler;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
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
            if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;
            if (!isPassive(entity)) return InteractionResult.PASS;

            boolean allowed = RegionPermissionChecker.isAllowed(serverPlayer, Rule.ATTACK_PASSIVE_MOBS, entity.blockPosition());
            if (allowed) return InteractionResult.PASS;

            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_ATTACK_PASSIVE_MOB, Color.RED);
            return InteractionResult.FAIL;
        });
    }

    private static boolean isPassive(Entity entity) {
        return entity instanceof Animal ||
                entity instanceof Bat ||
                entity instanceof Allay ||
                entity instanceof Axolotl ||
                entity instanceof WanderingTrader;
    }
}