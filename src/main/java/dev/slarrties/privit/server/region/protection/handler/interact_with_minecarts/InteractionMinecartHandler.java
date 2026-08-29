package dev.slarrties.privit.server.region.protection.handler.interact_with_minecarts;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.PlayerPermissionCache;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;
import dev.slarrties.privit.server.region.protection.handler.RuleEventHandler;

import net.minecraft.item.ItemStack;
import net.minecraft.item.MinecartItem;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.AbstractRailBlock;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;

@AssociatedRule(Rule.INTERACT_WITH_MINECARTS)
public final class InteractionMinecartHandler implements RuleEventHandler {

    @Override
    public Rule getRule() { return Rule.INTERACT_WITH_MINECARTS; }

    @Override
    public void register() {

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient) return ActionResult.PASS;
            if (!(player instanceof ServerPlayerEntity serverPlayer)) return ActionResult.PASS;

            ItemStack stack = serverPlayer.getStackInHand(hand);

            if (!(stack.getItem() instanceof MinecartItem)) return ActionResult.PASS;
            if (!(world.getBlockState(hitResult.getBlockPos()).getBlock() instanceof AbstractRailBlock)) return ActionResult.PASS;

            boolean allowed = RegionPermissionChecker.isAllowed(serverPlayer, Rule.INTERACT_WITH_MINECARTS, hitResult.getBlockPos());
            if (allowed) return ActionResult.PASS;

            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_INTERACT_MINECART, Color.RED);

            return ActionResult.FAIL;
        });

        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient) return ActionResult.PASS;
            if (!(player instanceof ServerPlayerEntity serverPlayer)) return ActionResult.PASS;
            if (!(entity instanceof AbstractMinecartEntity)) return ActionResult.PASS;

            BlockPos pos = entity.getBlockPos();
            boolean allowed = PlayerPermissionCache.isAllowed(serverPlayer, Rule.INTERACT_WITH_MINECARTS, pos);
            if (allowed) return ActionResult.PASS;

            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_INTERACT_MINECART, Color.RED);

            return ActionResult.FAIL;
        });
    }
}