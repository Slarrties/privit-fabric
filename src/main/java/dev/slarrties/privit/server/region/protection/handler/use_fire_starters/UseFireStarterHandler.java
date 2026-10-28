package dev.slarrties.privit.server.region.protection.handler.use_fire_starters;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.util.InventorySyncSystem;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.tracking.protection.FireOriginTracker;
import dev.slarrties.privit.server.tracking.protection.CampfireOriginTracker;
import dev.slarrties.privit.server.region.protection.handler.RuleEventHandler;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionResult;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;

@AssociatedRule(Rule.USE_FIRE_STARTERS)
public final class UseFireStarterHandler implements RuleEventHandler {

    @Override
    public Rule getRule() { return Rule.USE_FIRE_STARTERS; }

    @Override
    public void register() {
        UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;

            ItemStack stack = player.getItemInHand(hand);
            if (!isFireStarter(stack)) return InteractionResult.PASS;
            BlockPos checkPos = entity.blockPosition();

            boolean allowed = RegionPermissionChecker.isAllowed(serverPlayer, Rule.USE_FIRE_STARTERS, checkPos);
            if (!allowed) {
                sendDenyNotification(serverPlayer);
                InventorySyncSystem.syncHandSlot(serverPlayer, hand);
                return InteractionResult.FAIL;
            }

            if (level instanceof ServerLevel serverLevel) {
                FireOriginTracker fireTracker = WorldRegistry.get(serverLevel)
                        .getTrackerManager()
                        .getFireOriginTracker();
                fireTracker.record(checkPos, serverPlayer.getUUID());
            }

            return InteractionResult.PASS;
        });

        UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
            if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;

            ItemStack stack = player.getItemInHand(hand);
            if (!isFireStarter(stack)) return InteractionResult.PASS;
            BlockPos checkPos = hitResult.getBlockPos().offset(hitResult.getDirection().getUnitVec3i());

            boolean allowed = RegionPermissionChecker.isAllowed(serverPlayer, Rule.USE_FIRE_STARTERS, checkPos);
            if (!allowed) {
                sendDenyNotification(serverPlayer);
                InventorySyncSystem.syncHandSlot(serverPlayer, hand);
                return InteractionResult.FAIL;
            }

            BlockPos clickedPos = hitResult.getBlockPos();
            BlockState clickedState = level.getBlockState(clickedPos);

            if (level instanceof ServerLevel serverLevel) {
                if (clickedState.is(Blocks.CAMPFIRE) || clickedState.is(Blocks.SOUL_CAMPFIRE)) {
                    if (!clickedState.getValue(BlockStateProperties.LIT)) {
                        CampfireOriginTracker campfireTracker = WorldRegistry.get(serverLevel)
                                .getTrackerManager()
                                .getCampfireOriginTracker();
                        campfireTracker.record(clickedPos, serverPlayer.getUUID());
                    }
                } else {
                    FireOriginTracker fireTracker = WorldRegistry.get(serverLevel)
                            .getTrackerManager()
                            .getFireOriginTracker();
                    fireTracker.record(checkPos, serverPlayer.getUUID());
                }
            }
            return InteractionResult.PASS;
        });

        UseItemCallback.EVENT.register((player, level, hand) -> {
            if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;

            ItemStack stack = player.getItemInHand(hand);
            if (!isFireStarter(stack)) return InteractionResult.PASS;
            BlockPos checkPos = serverPlayer.blockPosition();

            boolean allowed = RegionPermissionChecker.isAllowed(serverPlayer, Rule.USE_FIRE_STARTERS, checkPos);
            if (!allowed) {
                sendDenyNotification(serverPlayer);
                InventorySyncSystem.syncHandSlot(serverPlayer, hand);
                return InteractionResult.FAIL;
            }

            if (level instanceof ServerLevel serverLevel) {
                FireOriginTracker fireTracker = WorldRegistry.get(serverLevel)
                        .getTrackerManager()
                        .getFireOriginTracker();
                fireTracker.record(checkPos, serverPlayer.getUUID());
            }

            return InteractionResult.PASS;
        });
    }

    private static boolean isFireStarter(ItemStack stack) {
        return stack.is(Items.FLINT_AND_STEEL) || stack.is(Items.FIRE_CHARGE);
    }

    private static void sendDenyNotification(ServerPlayer serverPlayer) {
        PlayerNotification.trySend(serverPlayer, NotificationType.DENY_USE_FIRE_STARTER, Color.RED);
    }
}