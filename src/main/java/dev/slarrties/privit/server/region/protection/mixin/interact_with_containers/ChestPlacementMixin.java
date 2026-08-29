package dev.slarrties.privit.server.region.protection.mixin.interact_with_containers;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.util.InventorySyncSystem;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;

import net.minecraft.world.World;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.enums.ChestType;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.network.ServerPlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@AssociatedRule(Rule.INTERACT_WITH_CONTAINERS)
@Mixin(ChestBlock.class)
public abstract class ChestPlacementMixin {

    @Inject(method = "getPlacementState", at = @At("HEAD"), cancellable = true)
    private void preventIllegalDoubleChest(ItemPlacementContext context, CallbackInfoReturnable<BlockState> cir) {
        World world = context.getWorld();
        if (world.isClient || !(context.getPlayer() instanceof ServerPlayerEntity serverPlayer)) return;

        BlockPos newChestPos = context.getBlockPos();
        BlockState newChestState = ((ChestBlock) (Object) this).getDefaultState()
                .with(ChestBlock.FACING, context.getHorizontalPlayerFacing().getOpposite());

        boolean shouldBlock = willFormDoubleChestWithProtectedChest(world, newChestPos, newChestState, serverPlayer);
        if (shouldBlock) {
            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_INTERACT_CONTAINER, Color.RED);
            InventorySyncSystem.syncHandSlot(serverPlayer, context.getHand());
            cir.setReturnValue(null);
        }
    }

    @Unique
    private boolean willFormDoubleChestWithProtectedChest(World world, BlockPos newChestPos, BlockState newChestState, ServerPlayerEntity player) {
        Direction facing = newChestState.get(ChestBlock.FACING);

        for (Direction dir : new Direction[]{facing.rotateYClockwise(), facing.rotateYCounterclockwise()}) {
            BlockPos neighborPos = newChestPos.offset(dir);
            BlockState neighborState = world.getBlockState(neighborPos);

            if (neighborState.getBlock() instanceof ChestBlock &&
                    neighborState.get(ChestBlock.FACING) == facing &&
                    neighborState.get(ChestBlock.CHEST_TYPE) == ChestType.SINGLE) {

                boolean allowed = RegionPermissionChecker.isAllowed(player, Rule.INTERACT_WITH_CONTAINERS, neighborPos);
                if (!allowed) return true;
            }
        }
        return false;
    }
}