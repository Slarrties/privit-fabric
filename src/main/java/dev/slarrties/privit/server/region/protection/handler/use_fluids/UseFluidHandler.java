package dev.slarrties.privit.server.region.protection.handler.use_fluids;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.tracking.context.EntitySpawnContext;
import dev.slarrties.privit.server.tracking.context.FluidPlacementContext;
import dev.slarrties.privit.server.region.util.InventorySyncSystem;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;
import dev.slarrties.privit.server.region.protection.handler.RuleEventHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.server.level.ServerPlayer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;

@AssociatedRule(Rule.USE_FLUIDS)
public final class UseFluidHandler implements RuleEventHandler {

    @Override
    public Rule getRule() {
        return Rule.USE_FLUIDS;
    }

    @Override
    public void register() {
        UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
            if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;

            ItemStack stack = player.getItemInHand(hand);
            if (!isLiquidBucket(stack)) return InteractionResult.PASS;

            BlockPos clickPos = hitResult.getBlockPos();
            BlockState clickedState = level.getBlockState(clickPos);
            BlockPos placePos;
            if (clickedState.getBlock() instanceof LiquidBlockContainer || clickedState.getBlock() instanceof AbstractCauldronBlock) {
                placePos = clickPos;
            } else {
                placePos = clickPos.offset(hitResult.getDirection().getUnitVec3i());
            }

            boolean allowed = RegionPermissionChecker.isAllowed(serverPlayer, Rule.USE_FLUIDS, placePos);
            if (!allowed) {
                sendDenyNotification(serverPlayer, stack);
                InventorySyncSystem.syncHandSlot(serverPlayer, hand);
                return InteractionResult.FAIL;
            }

            FluidPlacementContext.push(serverPlayer, placePos);
            if (isFishBucket(stack)) EntitySpawnContext.push(serverPlayer, Vec3.atCenterOf(placePos));

            return InteractionResult.PASS;
        });

        UseItemCallback.EVENT.register((player, level, hand) -> {
            if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;

            ItemStack stack = player.getItemInHand(hand);
            if (!isLiquidBucket(stack)) return InteractionResult.PASS;
            double reach = serverPlayer.blockInteractionRange();
            Vec3 start = serverPlayer.getEyePosition();
            Vec3 direction = serverPlayer.getLookAngle();
            Vec3 end = start.add(direction.scale(reach));
            BlockHitResult hitResult = level.clip(
                    new ClipContext(
                            start,
                            end,
                            ClipContext.Block.OUTLINE,
                            ClipContext.Fluid.ANY,
                            serverPlayer
                    )
            );
            BlockPos placePos;

            if (hitResult.getType() == HitResult.Type.BLOCK) {
                BlockPos clickPos = hitResult.getBlockPos();
                BlockState clickedState = level.getBlockState(clickPos);

                if (clickedState.getBlock() instanceof LiquidBlockContainer) {
                    placePos = clickPos;
                } else {
                    placePos = clickPos.offset(hitResult.getDirection().getUnitVec3i());
                }
            } else {
                return InteractionResult.FAIL;
            }

            boolean allowed = RegionPermissionChecker.isAllowed(serverPlayer, Rule.USE_FLUIDS, placePos);
            if (!allowed) {
                sendDenyNotification(serverPlayer, stack);
                InventorySyncSystem.syncHandSlot(serverPlayer, hand);
                return InteractionResult.FAIL;
            }

            return InteractionResult.PASS;
        });
    }

    private static boolean isLiquidBucket(ItemStack stack) {
        return stack.getItem() instanceof BucketItem;
    }

    private static boolean isFishBucket(ItemStack stack) {
        return stack.is(Items.TROPICAL_FISH_BUCKET) ||
                stack.is(Items.PUFFERFISH_BUCKET) ||
                stack.is(Items.SALMON_BUCKET) ||
                stack.is(Items.COD_BUCKET) ||
                stack.is(Items.AXOLOTL_BUCKET) ||
                stack.is(Items.TADPOLE_BUCKET);
    }

    private static void sendDenyNotification(ServerPlayer serverPlayer, ItemStack stack) {
        NotificationType type;

        if (stack.is(Items.LAVA_BUCKET)) {
            type = NotificationType.DENY_USE_LAVA_BUCKET;
        } else {
            type = NotificationType.DENY_USE_WATER_BUCKET;
        }

        PlayerNotification.trySend(serverPlayer, type, Color.RED);
    }
}