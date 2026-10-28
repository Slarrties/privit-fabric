package dev.slarrties.privit.server.region.protection.mixin.use_fluids;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;
import dev.slarrties.privit.server.tracking.protection.FluidOriginTracker;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.FlowingFluid;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@AssociatedRule(Rule.USE_FLUIDS)
@Mixin(FlowingFluid.class)
public abstract class FluidPropagateMixin {

    @Inject(method = "canMaybePassThrough", at = @At("HEAD"), cancellable = true)
    private void onCanFlow(BlockGetter level, BlockPos sourcePos, BlockState sourceState, Direction direction,
                           BlockPos testPos, BlockState testState, FluidState testFluidState,
                           CallbackInfoReturnable<Boolean> cir) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        FluidOriginTracker fluidOriginTracker = WorldRegistry.get(serverLevel)
                .getTrackerManager()
                .getFluidOriginTracker();

        UUID ownerUuid = fluidOriginTracker.getResponsible(sourcePos);
        if (ownerUuid == null) ownerUuid = findOwnerFromNeighbors(sourcePos, serverLevel);
        if (ownerUuid == null) return;

        boolean allowed = RegionPermissionChecker.isAllowed(ownerUuid, Rule.USE_FLUIDS, testPos, serverLevel);
        if (!allowed) {
            ServerPlayer serverPlayer = serverLevel.getServer()
                    .getPlayerList()
                    .getPlayer(ownerUuid);
            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_SPREAD_LIQUID, Color.RED);
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "getNewLiquid", at = @At("RETURN"))
    private void onFlowUpdated(ServerLevel level, BlockPos pos, BlockState state, CallbackInfoReturnable<FluidState> cir) {
        FluidState newState = cir.getReturnValue();
        FluidState oldFluid = state.getFluidState();

        FluidOriginTracker fluidOriginTracker = WorldRegistry.get(level)
                .getTrackerManager()
                .getFluidOriginTracker();

        if (newState.isEmpty()) {
            fluidOriginTracker.remove(pos);
            return;
        }

        UUID ownerUuid = fluidOriginTracker.getResponsible(pos);
        if (ownerUuid == null) ownerUuid = findOwnerFromNeighbors(pos, level);
        if (ownerUuid == null) return;
        if (!RegionPermissionChecker.isAllowed(ownerUuid, Rule.USE_FLUIDS, pos, level)) {
            fluidOriginTracker.remove(pos);
            return;
        }

        if (!oldFluid.isEmpty() && !newState.isEmpty()) {
            int oldLevel = oldFluid.getAmount();
            int newLevel = newState.getAmount();

            if (newLevel != oldLevel) {
                if (newLevel == 0) {
                    fluidOriginTracker.remove(pos);
                } else {
                    propagateToNeighbors(level, pos, newState);
                }
            }
        } else if (oldFluid.isEmpty() && !newState.isEmpty()) {
            handleFirstFluidAppearance(level, pos);
        }
    }

    @Unique
    private UUID findOwnerFromNeighbors(BlockPos pos, ServerLevel level) {
        FluidOriginTracker fluidOriginTracker = WorldRegistry.get(level)
                .getTrackerManager()
                .getFluidOriginTracker();
        for (Direction dir : Direction.values()) {
            UUID owner = fluidOriginTracker.getResponsible(pos.relative(dir));
            if (owner != null) return owner;
        }
        return null;
    }

    @Unique
    private void propagateToNeighbors(ServerLevel level, BlockPos pos, FluidState newState) {
        FluidOriginTracker fluidOriginTracker = WorldRegistry.get(level)
                .getTrackerManager()
                .getFluidOriginTracker();

        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            FluidState neighborFluid = level.getFluidState(neighborPos);

            if (!neighborFluid.isEmpty() && neighborFluid.getType() == newState.getType()) {
                fluidOriginTracker.propagate(pos, neighborPos);
            }
        }
    }

    @Unique
    private void handleFirstFluidAppearance(ServerLevel level, BlockPos pos) {
        FluidState currentFluid = level.getFluidState(pos);
        if (currentFluid.isEmpty()) return;

        FluidOriginTracker fluidOriginTracker = WorldRegistry.get(level)
                .getTrackerManager()
                .getFluidOriginTracker();
        BlockPos above = pos.above();
        FluidState aboveFluid = level.getFluidState(above);

        if (!aboveFluid.isEmpty() && aboveFluid.getType() == currentFluid.getType()) {
            fluidOriginTracker.propagate(above, pos);
            return;
        }
        propagateToNeighbors(level, pos, currentFluid);
    }
}