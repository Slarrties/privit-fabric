package dev.slarrties.privit.server.region.protection.handler.use_trial_mechanics;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.util.InventorySyncSystem;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.handler.RuleEventHandler;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;

import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.BlockState;
import net.minecraft.block.VaultBlock;
import net.minecraft.server.network.ServerPlayerEntity;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;

@AssociatedRule(Rule.USE_TRIAL_MECHANICS)
public final class InteractVaultHandler implements RuleEventHandler {

    @Override
    public Rule getRule() {
        return Rule.USE_TRIAL_MECHANICS;
    }

    @Override
    public void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient || !(player instanceof ServerPlayerEntity serverPlayer)) return ActionResult.PASS;

            ItemStack stack = player.getStackInHand(hand);
            if (!isTrialKey(stack)) return ActionResult.PASS;

            BlockPos pos = hitResult.getBlockPos();
            BlockState state = world.getBlockState(pos);

            if (!(state.getBlock() instanceof VaultBlock)) return ActionResult.PASS;

            boolean allowed = RegionPermissionChecker.isAllowed(serverPlayer, Rule.USE_TRIAL_MECHANICS, pos);
            if (allowed) return ActionResult.PASS;

            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_USE_TRIAL_MECHANICS, Color.RED);
            InventorySyncSystem.syncHandSlot(serverPlayer, hand);

            return ActionResult.FAIL;
        });
    }

    private static boolean isTrialKey(ItemStack stack) {
        return stack.isOf(Items.TRIAL_KEY) || stack.isOf(Items.OMINOUS_TRIAL_KEY);
    }
}