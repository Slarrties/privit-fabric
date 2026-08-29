package dev.slarrties.privit.server.region.protection.mixin.use_fluids;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;
import dev.slarrties.privit.server.tracking.protection.FluidOriginTracker;

import net.minecraft.world.World;
import net.minecraft.world.BlockView;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@AssociatedRule(Rule.USE_FLUIDS)
@Mixin(FlowableFluid.class)
public abstract class FluidPropagateMixin {

    @Inject(method = "canFlow", at = @At("HEAD"), cancellable = true)
    private void onCanFlow(BlockView world, BlockPos fromPos, BlockState fromState, Direction direction,
                           BlockPos toPos, BlockState toState, FluidState toFluidState, Fluid fluid,
                           CallbackInfoReturnable<Boolean> cir) {
        if (!(world instanceof ServerWorld serverWorld)) return;

        FluidOriginTracker fluidOriginTracker = WorldRegistry.get(serverWorld)
                .getTrackerManager()
                .getFluidOriginTracker();

        UUID ownerUuid = fluidOriginTracker.getOwner(fromPos);
        if (ownerUuid == null) ownerUuid = findOwnerFromNeighbors(fromPos, serverWorld);
        if (ownerUuid == null) return;

        boolean allowed = RegionPermissionChecker.isAllowed(ownerUuid, Rule.USE_FLUIDS, toPos, serverWorld);
        if (!allowed) {
            ServerPlayerEntity serverPlayer = serverWorld.getServer()
                    .getPlayerManager()
                    .getPlayer(ownerUuid);
            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_SPREAD_LIQUID, Color.RED);
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "getUpdatedState", at = @At("RETURN"))
    private void onFlowUpdated(World world, BlockPos pos, BlockState state, CallbackInfoReturnable<FluidState> cir) {
        if (world.isClient() || !(world instanceof ServerWorld serverWorld)) return;

        FluidState newState = cir.getReturnValue();
        FluidOriginTracker fluidOriginTracker = WorldRegistry.get(serverWorld)
                .getTrackerManager()
                .getFluidOriginTracker();

        if (newState.isEmpty()) {
            fluidOriginTracker.remove(pos);
            return;
        }

        UUID ownerUuid = fluidOriginTracker.getOwner(pos);
        if (ownerUuid == null) ownerUuid = findOwnerFromNeighbors(pos, serverWorld);
        if (ownerUuid == null) return;

        if (!RegionPermissionChecker.isAllowed(ownerUuid, Rule.USE_FLUIDS, pos, serverWorld)) {
            fluidOriginTracker.remove(pos);
            return;
        }

        propagateToNeighbors(serverWorld, pos, newState);
    }

    @Unique
    private UUID findOwnerFromNeighbors(BlockPos pos, ServerWorld world) {
        FluidOriginTracker fluidOriginTracker = WorldRegistry.get(world)
                .getTrackerManager()
                .getFluidOriginTracker();

        for (Direction dir : Direction.values()) {
            UUID owner = fluidOriginTracker.getOwner(pos.offset(dir));
            if (owner != null) return owner;
        }
        return null;
    }

    @Unique
    private void propagateToNeighbors(ServerWorld world, BlockPos pos, FluidState newState) {
        FluidOriginTracker fluidOriginTracker = WorldRegistry.get(world)
                .getTrackerManager()
                .getFluidOriginTracker();

        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.offset(dir);
            FluidState neighborFluid = world.getFluidState(neighborPos);

            if (!neighborFluid.isEmpty() && neighborFluid.getFluid() == newState.getFluid()) {
                fluidOriginTracker.propagate(pos, neighborPos);
            }
        }
    }
}