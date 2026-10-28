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

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonStructureResolver;

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
@Mixin(PistonBaseBlock.class)
public abstract class PistonBlockProtectionMixin {

    @Final @Shadow
    private boolean isSticky;

    @Inject(method = "triggerEvent", at = @At("HEAD"), cancellable = true)
    private void handlePistonSyncedBlockEvent(BlockState state, Level level, BlockPos pos, int b0, int b1,
                                              CallbackInfoReturnable<Boolean> cir) {
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) return;

        if (b0 == 0 || b0 == 1 || b0 == 2) {
            UUID responsible = RedstoneReceiverHandler.findResponsiblePlayer(serverLevel, pos);
            if (responsible != null) {
                PistonMovementContext.push(responsible, pos);
                BlockFallOriginTracker blockFallTracker = WorldRegistry.get(serverLevel)
                        .getTrackerManager()
                        .getBlockFallOriginTracker();
                blockFallTracker.record(pos, responsible);
                BlockFallContext.push(responsible, pos);
            }
        }

        if ((b0 == 1 || b0 == 2) && this.isSticky) {
            Direction dir = state.getValue(PistonBaseBlock.FACING);
            BlockPos pulledPos = pos.relative(dir).relative(dir);

            UUID responsible = RedstoneReceiverHandler.findResponsiblePlayer(serverLevel, pos);
            if (responsible == null) {
                PistonMovementContext context = PistonMovementContext.getCurrent();
                if (context != null) {
                    responsible = context.getResponsible();
                }
            }

            if (responsible != null && !RegionPermissionChecker.isAllowed(responsible, Rule.USE_PISTONS, pulledPos, serverLevel)) {
                denyRetraction(serverLevel, pos, state, responsible);
                cir.setReturnValue(false);
            }
        }
    }


    @Inject(
            method = "moveBlocks",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/piston/PistonStructureResolver;resolve()Z",
                    shift = At.Shift.AFTER
            ),
            cancellable = true
    )
    private void checkPistonMovement(Level level, BlockPos pistonPos, Direction direction, boolean extending,
                                     CallbackInfoReturnable<Boolean> cir,
                                     @Local(ordinal = 0) PistonStructureResolver resolver) {
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) return;

        PistonMovementContext context = PistonMovementContext.getCurrent();
        if (context == null) return;

        UUID responsible = context.getResponsible();
        if (responsible == null) return;
        if (extending) return;

        BlockPos headTarget = pistonPos.relative(direction);
        if (!RegionPermissionChecker.isAllowed(responsible, Rule.USE_PISTONS, headTarget, serverLevel)) {
            denyMovement(serverLevel, pistonPos, responsible);
            cir.setReturnValue(false);
            return;
        }

        if (resolver != null && resolver.resolve()) {
            Direction pushDirection = direction.getOpposite();

            for (BlockPos movedPos : resolver.getToPush()) {
                BlockPos destination = movedPos.relative(pushDirection);
                if (!RegionPermissionChecker.isAllowed(responsible, Rule.USE_PISTONS, destination, serverLevel)) {
                    denyMovement(serverLevel, pistonPos, responsible);
                    cir.setReturnValue(false);
                    return;
                }
            }
        }
    }

    @Inject(
            method = "moveBlocks",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/piston/PistonStructureResolver;resolve()Z",
                    shift = At.Shift.AFTER
            )
    )
    private void markFallFromPiston(Level level, BlockPos pistonPos, Direction direction, boolean extending,
                                    CallbackInfoReturnable<Boolean> cir,
                                    @Local(ordinal = 0) PistonStructureResolver resolver) {
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) return;

        PistonMovementContext context = PistonMovementContext.getCurrent();
        if (context == null || context.getResponsible() == null) return;
        if (resolver == null) return;

        UUID uuid = context.getResponsible();
        BlockFallOriginTracker blockFallTracker = WorldRegistry.get(serverLevel)
                .getTrackerManager()
                .getBlockFallOriginTracker();
        Direction pushDirection = extending ? direction : direction.getOpposite();

        for (BlockPos movedPos : resolver.getToPush()) {
            blockFallTracker.record(movedPos, uuid);
            blockFallTracker.record(movedPos.relative(pushDirection), uuid);
        }

        for (BlockPos brokenPos : resolver.getToDestroy()) {
            blockFallTracker.record(brokenPos, uuid);
        }
    }

    @Inject(method = "triggerEvent", at = @At("RETURN"))
    private void popPistonContext(BlockState state, Level level, BlockPos pos, int b0, int b1,
                                  CallbackInfoReturnable<Boolean> cir) {
        if (level.isClientSide()) return;
        if (b0 == 0 || b0 == 1 || b0 == 2) {
            PistonMovementContext.pop();
            BlockFallContext.pop();
        }
    }

    @Unique
    private void denyMovement(ServerLevel serverLevel, BlockPos pos, UUID playerUuid) {
        BlockState state = serverLevel.getBlockState(pos);

        serverLevel.setBlock(pos, state.setValue(PistonBaseBlock.EXTENDED, false), 3);
        serverLevel.sendBlockUpdated(pos, state, state, 3);

        ServerPlayer serverPlayer = serverLevel.getServer()
                .getPlayerList()
                .getPlayer(playerUuid);
        PlayerNotification.trySend(serverPlayer, NotificationType.DENY_USE_PISTON, Color.RED);
    }

    @Unique
    private void denyRetraction(ServerLevel serverLevel, BlockPos pos, BlockState state, UUID playerUuid) {
        Direction dir = state.getValue(PistonBaseBlock.FACING);
        BlockState retractedState = state.setValue(PistonBaseBlock.EXTENDED, false);
        serverLevel.setBlock(pos, retractedState, 3);

        BlockPos headPos = pos.relative(dir);
        if (serverLevel.getBlockState(headPos).is(Blocks.MOVING_PISTON)) {
            serverLevel.removeBlock(headPos, false);
        }
        serverLevel.sendBlockUpdated(pos, state, retractedState, 3);

        ServerPlayer serverPlayer = serverLevel.getServer()
                .getPlayerList()
                .getPlayer(playerUuid);
        PlayerNotification.trySend(serverPlayer, NotificationType.DENY_USE_PISTON, Color.RED);
        PistonMovementContext.pop();
        BlockFallContext.pop();
    }
}