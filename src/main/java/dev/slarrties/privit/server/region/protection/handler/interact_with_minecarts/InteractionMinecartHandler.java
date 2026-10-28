package dev.slarrties.privit.server.region.protection.handler.interact_with_minecarts;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.PlayerPermissionCache;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;
import dev.slarrties.privit.server.region.protection.handler.RuleEventHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MinecartItem;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.server.level.ServerPlayer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;

@AssociatedRule(Rule.INTERACT_WITH_MINECARTS)
public final class InteractionMinecartHandler implements RuleEventHandler {

    @Override
    public Rule getRule() { return Rule.INTERACT_WITH_MINECARTS; }

    @Override
    public void register() {
        UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
            if (level.isClientSide()) return InteractionResult.PASS;
            if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;

            ItemStack stack = serverPlayer.getItemInHand(hand);
            if (!(stack.getItem() instanceof MinecartItem)) return InteractionResult.PASS;
            if (!(level.getBlockState(hitResult.getBlockPos()).getBlock() instanceof BaseRailBlock)) return InteractionResult.PASS;

            boolean allowed = RegionPermissionChecker.isAllowed(serverPlayer, Rule.INTERACT_WITH_MINECARTS, hitResult.getBlockPos());
            if (allowed) return InteractionResult.PASS;

            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_INTERACT_MINECART, Color.RED);
            return InteractionResult.FAIL;
        });

        UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            if (level.isClientSide()) return InteractionResult.PASS;
            if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;
            if (!(entity instanceof AbstractMinecart)) return InteractionResult.PASS;

            BlockPos pos = entity.blockPosition();
            boolean allowed = PlayerPermissionCache.isAllowed(serverPlayer, Rule.INTERACT_WITH_MINECARTS, pos);
            if (allowed) return InteractionResult.PASS;

            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_INTERACT_MINECART, Color.RED);
            return InteractionResult.FAIL;
        });
    }
}