package dev.slarrties.privit.server.region.protection.mixin.interact_with_containers;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.util.InventorySyncSystem;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.item.context.BlockPlaceContext;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@AssociatedRule(Rule.INTERACT_WITH_CONTAINERS)
@Mixin(ChestBlock.class)
public abstract class ChestPlacementMixin {

    @Inject(method = "getStateForPlacement", at = @At("HEAD"), cancellable = true)
    private void preventIllegalDoubleChest(BlockPlaceContext context, CallbackInfoReturnable<BlockState> cir) {
        Level level = context.getLevel();
        if (level.isClientSide() || !(context.getPlayer() instanceof ServerPlayer serverPlayer)) return;

        BlockPos newChestPos = context.getClickedPos();
        BlockState newChestState = ((ChestBlock) (Object) this).defaultBlockState()
                .setValue(ChestBlock.FACING, context.getHorizontalDirection().getOpposite());

        boolean shouldBlock = willFormDoubleChestWithProtectedChest(level, newChestPos, newChestState, serverPlayer);

        if (shouldBlock) {
            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_INTERACT_CONTAINER, Color.RED);
            InventorySyncSystem.syncHandSlot(serverPlayer, context.getHand());
            cir.setReturnValue(null);
        }
    }

    @Unique
    private boolean willFormDoubleChestWithProtectedChest(Level level, BlockPos newChestPos, BlockState newChestState, ServerPlayer serverPlayer) {
        Direction facing = newChestState.getValue(ChestBlock.FACING);

        for (Direction dir : new Direction[]{facing.getClockWise(), facing.getCounterClockWise()}) {
            BlockPos neighborPos = newChestPos.relative(dir);
            BlockState neighborState = level.getBlockState(neighborPos);

            if (neighborState.getBlock() instanceof ChestBlock &&
                    neighborState.getValue(ChestBlock.FACING) == facing &&
                    neighborState.getValue(ChestBlock.TYPE) == ChestType.SINGLE) {

                boolean allowed = RegionPermissionChecker.isAllowed(serverPlayer.getUUID(), Rule.INTERACT_WITH_CONTAINERS, neighborPos, serverPlayer.level());
                if (!allowed) return true;
            }
        }
        return false;
    }
}