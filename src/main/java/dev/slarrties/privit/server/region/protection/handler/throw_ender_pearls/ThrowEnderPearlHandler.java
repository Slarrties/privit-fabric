package dev.slarrties.privit.server.region.protection.handler.throw_ender_pearls;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.util.InventorySyncSystem;
import dev.slarrties.privit.server.region.protection.*;
import dev.slarrties.privit.server.region.protection.handler.RuleEventHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;
import net.fabricmc.fabric.api.event.player.UseItemCallback;


@AssociatedRule(Rule.THROW_ENDER_PEARLS)
public class ThrowEnderPearlHandler implements RuleEventHandler {

    @Override
    public Rule getRule() { return Rule.THROW_ENDER_PEARLS; }

    public void register() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (world.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;

            ItemStack stack = player.getItemInHand(hand);
            if (!stack.is(Items.ENDER_PEARL)) return InteractionResult.PASS;

            BlockPos pos = player.blockPosition();
            boolean allowed = RegionPermissionChecker.isAllowed(serverPlayer, Rule.THROW_ENDER_PEARLS, pos);
            if (allowed) return InteractionResult.PASS;

            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_THROW_ENDER_PEARL, Color.RED);
            InventorySyncSystem.syncHandSlot(serverPlayer, hand);

            return InteractionResult.FAIL;
        });
    }
}