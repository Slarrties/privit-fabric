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

import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.item.BucketItem;
import net.minecraft.block.BlockState;
import net.minecraft.block.Waterloggable;
import net.minecraft.util.ActionResult;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.RaycastContext;
import net.minecraft.server.network.ServerPlayerEntity;
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

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient || !(player instanceof ServerPlayerEntity serverPlayer))
                return ActionResult.PASS;

            ItemStack stack = player.getStackInHand(hand);
            if (!isLiquidBucket(stack)) return ActionResult.PASS;

            BlockPos clickPos = hitResult.getBlockPos();
            BlockState clickedState = world.getBlockState(clickPos);
            BlockPos placePos;

            if (clickedState.getBlock() instanceof Waterloggable
                    || clickedState.getBlock() instanceof net.minecraft.block.AbstractCauldronBlock) {
                placePos = clickPos;
            } else {
                placePos = clickPos.offset(hitResult.getSide());
            }

            boolean allowed = RegionPermissionChecker.isAllowed(serverPlayer, Rule.USE_FLUIDS, placePos);
            if (!allowed) {
                sendDenyNotification(serverPlayer, stack);
                InventorySyncSystem.syncHandSlot(serverPlayer, hand);
                return ActionResult.FAIL;
            }

            FluidPlacementContext.push(serverPlayer, placePos);
            if (isFishBucket(stack)) EntitySpawnContext.push(serverPlayer, Vec3d.ofCenter(placePos));

            return ActionResult.PASS;
        });

        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (world.isClient || !(player instanceof ServerPlayerEntity serverPlayer))
                return TypedActionResult.pass(player.getStackInHand(hand));

            ItemStack stack = player.getStackInHand(hand);
            if (!isLiquidBucket(stack)) return TypedActionResult.pass(stack);

            double reach = serverPlayer.getBlockInteractionRange();
            Vec3d start = serverPlayer.getEyePos();
            Vec3d direction = serverPlayer.getRotationVec(1.0F);
            Vec3d end = start.add(direction.multiply(reach));
            BlockHitResult hitResult = world.raycast(
                    new RaycastContext(
                            start,
                            end,
                            RaycastContext.ShapeType.OUTLINE,
                            RaycastContext.FluidHandling.ANY,
                            serverPlayer
                    )
            );
            BlockPos placePos;

            if (hitResult.getType() == HitResult.Type.BLOCK) {
                BlockPos clickPos = hitResult.getBlockPos();
                BlockState clickedState = world.getBlockState(clickPos);

                if (clickedState.getBlock() instanceof Waterloggable) {
                    placePos = clickPos;
                } else {
                    placePos = clickPos.offset(hitResult.getSide());
                }
            } else {
                return TypedActionResult.fail(stack);
            }

            boolean allowed = RegionPermissionChecker.isAllowed(serverPlayer, Rule.USE_FLUIDS, placePos);

            if (!allowed) {
                sendDenyNotification(serverPlayer, stack);
                InventorySyncSystem.syncHandSlot(serverPlayer, hand);
                return TypedActionResult.fail(stack);
            }

            return TypedActionResult.pass(stack);
        });
    }

    private static boolean isLiquidBucket(ItemStack stack) {
        return stack.getItem() instanceof BucketItem;
    }

    private static boolean isFishBucket(ItemStack stack) {
        return stack.isOf(Items.TROPICAL_FISH_BUCKET) ||
                stack.isOf(Items.PUFFERFISH_BUCKET) ||
                stack.isOf(Items.SALMON_BUCKET) ||
                stack.isOf(Items.COD_BUCKET) ||
                stack.isOf(Items.AXOLOTL_BUCKET) ||
                stack.isOf(Items.TADPOLE_BUCKET);
    }

    private static void sendDenyNotification(ServerPlayerEntity serverPlayer, ItemStack stack) {
        NotificationType type;

        if (stack.isOf(Items.LAVA_BUCKET)) {
            type = NotificationType.DENY_USE_LAVA_BUCKET;
        } else {
            type = NotificationType.DENY_USE_WATER_BUCKET;
        }

        PlayerNotification.trySend(serverPlayer, type, Color.RED);
    }
}