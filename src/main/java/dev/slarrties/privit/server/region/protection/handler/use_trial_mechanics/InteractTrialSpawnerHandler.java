package dev.slarrties.privit.server.region.protection.handler.use_trial_mechanics;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.util.InventorySyncSystem;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;
import dev.slarrties.privit.server.region.protection.handler.RuleEventHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.TrialSpawnerBlock;
import net.minecraft.server.level.ServerPlayer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;

@AssociatedRule(Rule.USE_TRIAL_MECHANICS)
public final class InteractTrialSpawnerHandler implements RuleEventHandler {

    @Override
    public Rule getRule() { return Rule.USE_TRIAL_MECHANICS; }

    @Override
    public void register() {
        UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
            if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;

            ItemStack stack = player.getItemInHand(hand);
            if (!isSpawnEgg(stack)) return InteractionResult.PASS;

            BlockPos pos = hitResult.getBlockPos();
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof TrialSpawnerBlock)) return InteractionResult.PASS;

            boolean allowed = RegionPermissionChecker.isAllowed(serverPlayer, Rule.USE_TRIAL_MECHANICS, pos);
            if (allowed) return InteractionResult.PASS;

            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_USE_TRIAL_MECHANICS, Color.RED);
            InventorySyncSystem.syncHandSlot(serverPlayer, hand);
            return InteractionResult.FAIL;
        });
    }

    private static boolean isSpawnEgg(ItemStack stack) {
        return stack.getItem() instanceof SpawnEggItem;
    }
}