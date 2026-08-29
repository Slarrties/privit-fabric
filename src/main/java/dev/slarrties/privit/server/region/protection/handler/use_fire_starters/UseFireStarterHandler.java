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

import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.TypedActionResult;
import net.minecraft.block.Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.state.property.Properties;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;

@AssociatedRule(Rule.USE_FIRE_STARTERS)
public final class UseFireStarterHandler implements RuleEventHandler {

    @Override
    public Rule getRule() {
        return Rule.USE_FIRE_STARTERS;
    }

    @Override
    public void register() {

        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient || !(player instanceof ServerPlayerEntity serverPlayer))
                return ActionResult.PASS;

            ItemStack stack = player.getStackInHand(hand);
            if (!isFireStarter(stack)) return ActionResult.PASS;

            BlockPos checkPos = entity.getBlockPos();
            boolean allowed = RegionPermissionChecker.isAllowed(serverPlayer, Rule.USE_FIRE_STARTERS, checkPos);

            if (!allowed) {
                sendDenyNotification(serverPlayer);
                InventorySyncSystem.syncHandSlot(serverPlayer, hand);
                return ActionResult.FAIL;
            }

            if(world instanceof ServerWorld serverWorld) {
                FireOriginTracker fireTracker =  WorldRegistry.get(serverWorld)
                        .getTrackerManager()
                        .getFireOriginTracker();
                fireTracker.record(checkPos, serverPlayer.getUuid());
            }

            return ActionResult.PASS;
        });

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient || !(player instanceof ServerPlayerEntity serverPlayer))
                return ActionResult.PASS;

            ItemStack stack = player.getStackInHand(hand);
            if (!isFireStarter(stack)) return ActionResult.PASS;

            BlockPos checkPos = hitResult.getBlockPos().offset(hitResult.getSide());
            boolean allowed = RegionPermissionChecker.isAllowed(serverPlayer, Rule.USE_FIRE_STARTERS, checkPos);

            if (!allowed) {
                sendDenyNotification(serverPlayer);
                InventorySyncSystem.syncHandSlot(serverPlayer, hand);
                return ActionResult.FAIL;
            }

            BlockPos clickedPos = hitResult.getBlockPos();
            BlockState clickedState = world.getBlockState(clickedPos);

            if(world instanceof ServerWorld serverWorld) {
                if (clickedState.isOf(Blocks.CAMPFIRE) || clickedState.isOf(Blocks.SOUL_CAMPFIRE)) {
                    if (!clickedState.get(Properties.LIT)) {
                        CampfireOriginTracker campfireTracker = WorldRegistry.get(serverWorld)
                                .getTrackerManager()
                                .getCampfireOriginTracker();
                        campfireTracker.record(clickedPos, serverPlayer.getUuid());
                    }
                } else {
                    FireOriginTracker fireTracker = WorldRegistry.get(serverWorld)
                            .getTrackerManager()
                            .getFireOriginTracker();
                    fireTracker.record(checkPos, serverPlayer.getUuid());
                }
            }

            return ActionResult.PASS;
        });

        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (world.isClient || !(player instanceof ServerPlayerEntity serverPlayer))
                return TypedActionResult.pass(player.getStackInHand(hand));

            ItemStack stack = player.getStackInHand(hand);
            if (!isFireStarter(stack)) return TypedActionResult.pass(stack);

            BlockPos checkPos = serverPlayer.getBlockPos();
            boolean allowed = RegionPermissionChecker.isAllowed(serverPlayer, Rule.USE_FIRE_STARTERS, checkPos);

            if (!allowed) {
                sendDenyNotification(serverPlayer);
                InventorySyncSystem.syncHandSlot(serverPlayer, hand);
                return TypedActionResult.fail(stack);
            }

            if(world instanceof ServerWorld serverWorld) {
                FireOriginTracker fireTracker = WorldRegistry.get(serverWorld)
                        .getTrackerManager()
                        .getFireOriginTracker();
                fireTracker.record(checkPos, serverPlayer.getUuid());
            }

            return TypedActionResult.pass(stack);
        });
    }

    private static boolean isFireStarter(ItemStack stack) {
        return stack.isOf(Items.FLINT_AND_STEEL) || stack.isOf(Items.FIRE_CHARGE);
    }

    private static void sendDenyNotification(ServerPlayerEntity serverPlayer) {
        PlayerNotification.trySend(serverPlayer, NotificationType.DENY_USE_FIRE_STARTER, Color.RED);
    }
}