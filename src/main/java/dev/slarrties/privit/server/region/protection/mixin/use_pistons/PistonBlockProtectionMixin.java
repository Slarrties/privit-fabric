package dev.slarrties.privit.server.region.protection.mixin.use_pistons;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;
import dev.slarrties.privit.server.tracking.context.BlockFallContext;
import dev.slarrties.privit.server.tracking.context.PistonMovementContext;
import dev.slarrties.privit.server.tracking.protection.BlockFallOriginTracker;
import dev.slarrties.privit.server.tracking.redstone.handler.RedstoneReceiverHandler;

import net.minecraft.world.World;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.block.Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.PistonBlock;
import net.minecraft.block.piston.PistonHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;

import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@AssociatedRule({Rule.USE_PISTONS, Rule.CAUSE_BLOCK_FALL})
@Mixin(PistonBlock.class)
public abstract class PistonBlockProtectionMixin {

    @Final @Shadow private boolean sticky;

    @Inject(method = "onSyncedBlockEvent", at = @At("HEAD"), cancellable = true)
    private void handlePistonSyncedBlockEvent(BlockState state, World world, BlockPos pos, int type, int data,
                                              CallbackInfoReturnable<Boolean> cir) {
        if (world.isClient() || !(world instanceof ServerWorld serverWorld)) return;
        if (type == 0 || type == 1 || type == 2) {
            UUID responsible = RedstoneReceiverHandler.findResponsiblePlayer(serverWorld, pos);

            if (responsible != null) {
                PistonMovementContext.push(responsible, pos);
                BlockFallOriginTracker blockFallTracker = WorldRegistry.get(serverWorld)
                        .getTrackerManager()
                        .getBlockFallOriginTracker();
                blockFallTracker.record(pos, responsible);
                BlockFallContext.push(responsible, pos);
            }
        }

        if ((type == 1 || type == 2) && this.sticky) {
            Direction dir = state.get(PistonBlock.FACING);
            BlockPos pulledPos = pos.offset(dir).offset(dir);
            UUID responsible = RedstoneReceiverHandler.findResponsiblePlayer(serverWorld, pos);

            if (responsible == null) {
                PistonMovementContext context = PistonMovementContext.getCurrent();
                if (context != null) {
                    responsible = context.getResponsible();
                }
            }

            if (!RegionPermissionChecker.isAllowed(responsible, Rule.USE_PISTONS, pulledPos, serverWorld)) {
                denyRetraction(serverWorld, pos, state, responsible);
                cir.setReturnValue(false);
            }
        }
    }

    @Inject(
            method = "move",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/block/piston/PistonHandler;calculatePush()Z",
                    shift = At.Shift.AFTER
            ),
            cancellable = true
    )
    private void checkPistonMovement(World world, BlockPos pos, Direction dir, boolean retract,
                                     CallbackInfoReturnable<Boolean> cir,
                                     @Local(ordinal = 0) PistonHandler pistonHandler) {
        if (world.isClient() || !(world instanceof ServerWorld serverWorld)) return;

        PistonMovementContext context = PistonMovementContext.getCurrent();
        if (context == null) return;

        UUID responsible = context.getResponsible();
        if (responsible == null) return;
        if (!retract) return;

        BlockPos headTarget = pos.offset(dir);
        if (!RegionPermissionChecker.isAllowed(responsible, Rule.USE_PISTONS, headTarget, serverWorld)) {
            denyMovement(serverWorld, pos, responsible);
            cir.setReturnValue(false);
            return;
        }

        if (pistonHandler != null && pistonHandler.calculatePush()) {
            for (BlockPos movedPos : pistonHandler.getMovedBlocks()) {
                BlockPos destination = movedPos.offset(dir);
                if (!RegionPermissionChecker.isAllowed(responsible, Rule.USE_PISTONS, destination, serverWorld)) {
                    denyMovement(serverWorld, pos, responsible);
                    cir.setReturnValue(false);
                    return;
                }
            }
        }
    }

    @Inject(
            method = "move",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/block/piston/PistonHandler;calculatePush()Z",
                    shift = At.Shift.AFTER
            )
    )
    private void markFallFromPiston(World world, BlockPos pos, Direction dir, boolean retract,
                                    CallbackInfoReturnable<Boolean> cir,
                                    @Local(ordinal = 0) PistonHandler pistonHandler) {
        if (world.isClient() || !(world instanceof ServerWorld serverWorld)) return;

        PistonMovementContext context = PistonMovementContext.getCurrent();
        if (context == null || context.getResponsible() == null) return;
        if (pistonHandler == null) return;

        UUID uuid = context.getResponsible();
        BlockFallOriginTracker blockFallTracker = WorldRegistry.get(serverWorld)
                .getTrackerManager()
                .getBlockFallOriginTracker();

        for (BlockPos movedPos : pistonHandler.getMovedBlocks()) {
            blockFallTracker.record(movedPos, uuid);
            blockFallTracker.record(movedPos.offset(dir), uuid);
        }

        for (BlockPos brokenPos : pistonHandler.getBrokenBlocks()) {
            blockFallTracker.record(brokenPos, uuid);
        }
    }

    @Inject(method = "onSyncedBlockEvent", at = @At("RETURN"))
    private void popPistonContext(BlockState state, World world, BlockPos pos, int type, int data,
                                  CallbackInfoReturnable<Boolean> cir) {
        if (world.isClient()) return;

        if (type == 0 || type == 1 || type == 2) {
            PistonMovementContext.pop();
            BlockFallContext.pop();
        }
    }

    @Unique
    private void denyMovement(ServerWorld serverWorld, BlockPos pos, UUID playerUuid) {
        BlockState state = serverWorld.getBlockState(pos);
        serverWorld.setBlockState(pos, state.with(PistonBlock.EXTENDED, false), 3);
        serverWorld.updateListeners(pos, state, state, 3);
        ServerPlayerEntity serverPlayer = serverWorld.getServer()
                .getPlayerManager()
                .getPlayer(playerUuid);
        PlayerNotification.trySend(serverPlayer, NotificationType.DENY_USE_PISTON, Color.RED);
    }

    @Unique
    private void denyRetraction(ServerWorld serverWorld, BlockPos pos, BlockState state, UUID playerUuid) {
        Direction dir = state.get(PistonBlock.FACING);
        BlockState retractedState = state.with(PistonBlock.EXTENDED, false);
        serverWorld.setBlockState(pos, retractedState, 3);
        BlockPos headPos = pos.offset(dir);

        if (serverWorld.getBlockState(headPos).isOf(Blocks.MOVING_PISTON)) {
            serverWorld.removeBlock(headPos, false);
        }

        serverWorld.updateListeners(pos, state, retractedState, 3);
        ServerPlayerEntity serverPlayer = serverWorld.getServer()
                .getPlayerManager()
                .getPlayer(playerUuid);
        PlayerNotification.trySend(serverPlayer, NotificationType.DENY_USE_PISTON, Color.RED);
        PistonMovementContext.pop();
        BlockFallContext.pop();
    }
}