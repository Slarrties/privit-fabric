package dev.slarrties.privit.server.region.protection.handler.use_leashes;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.util.InventorySyncSystem;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.handler.RuleEventHandler;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.Leashable;
import net.minecraft.server.level.ServerPlayer;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;

@AssociatedRule(Rule.USE_LEASHES)
public final class UseLeashHandler implements RuleEventHandler {

    @Override
    public Rule getRule() { return Rule.USE_LEASHES; }

    @Override
    public void register() {
        UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            if (level.isClientSide()) return InteractionResult.PASS;
            if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;
            if (serverPlayer.getItemInHand(hand).getItem() != Items.LEAD) return InteractionResult.PASS;
            if (!(entity instanceof Leashable leashable) || !leashable.canBeLeashed()) return InteractionResult.PASS;

            boolean allowed = RegionPermissionChecker.isAllowed(serverPlayer, Rule.USE_LEASHES, entity.blockPosition());
            if (allowed) return InteractionResult.PASS;

            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_USE_LEASH, Color.RED);
            InventorySyncSystem.syncHandSlot(serverPlayer, hand);
            return InteractionResult.FAIL;
        });
    }
}