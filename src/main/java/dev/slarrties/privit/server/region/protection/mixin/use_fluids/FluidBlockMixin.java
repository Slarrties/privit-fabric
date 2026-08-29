package dev.slarrties.privit.server.region.protection.mixin.use_fluids;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.tracking.protection.FluidOriginTracker;
import dev.slarrties.privit.server.tracking.context.FluidPlacementContext;

import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemStack;
import net.minecraft.block.FluidBlock;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@AssociatedRule(Rule.USE_FLUIDS)
@Mixin(FluidBlock.class)
public abstract class FluidBlockMixin {

    @Inject(method = "onBlockAdded", at = @At("HEAD"))
    private void onAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify, CallbackInfo ci) {
        if (!(world instanceof ServerWorld serverWorld)) return;
        FluidPlacementContext context = FluidPlacementContext.getCurrent();

        if (context != null) {
            ServerPlayerEntity placer = context.getPlayer();

            if (placer != null) {
                FluidOriginTracker fluidOriginTracker = WorldRegistry.get(serverWorld)
                        .getTrackerManager()
                        .getFluidOriginTracker();
                fluidOriginTracker.record(pos, placer.getUuid());
            }
        }
        FluidPlacementContext.pop();
    }

    @Inject(method = "tryDrainFluid", at = @At("HEAD"))
    private void onLiquidDrained(PlayerEntity player, WorldAccess world, BlockPos pos, BlockState state, CallbackInfoReturnable<ItemStack> cir) {
        if (!(world instanceof ServerWorld serverWorld)) return;
        FluidOriginTracker fluidOriginTracker = WorldRegistry.get(serverWorld)
                .getTrackerManager()
                .getFluidOriginTracker();
        fluidOriginTracker.remove(pos);
    }

    @Inject(method = "getStateForNeighborUpdate", at = @At("RETURN"))
    private void onNeighborFluidUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world,
                                       BlockPos pos, BlockPos neighborPos, CallbackInfoReturnable<BlockState> cir) {
        if (!(world instanceof ServerWorld serverWorld)) return;

        BlockState newState = cir.getReturnValue();
        FluidState oldFluid = state.getFluidState();
        FluidState newFluid = newState.getFluidState();
        FluidOriginTracker fluidOriginTracker = WorldRegistry.get(serverWorld)
                .getTrackerManager()
                .getFluidOriginTracker();

        if (!oldFluid.isEmpty() && newFluid.isEmpty()) {
            fluidOriginTracker.remove(pos);
            return;
        }

        if (oldFluid.isEmpty() && !newFluid.isEmpty()) {
            handleFirstFluidAppearance(world, pos);
            return;
        }

        if (!oldFluid.isEmpty() && !newFluid.isEmpty()) {
            int oldLevel = oldFluid.getLevel();
            int newLevel = newFluid.getLevel();

            if (newLevel != oldLevel) {
                if (newLevel == 0) {
                    fluidOriginTracker.remove(pos);
                } else {
                    propagateFromNeighbors(world, pos);
                }
            }
        }
    }

    @Unique
    private void handleFirstFluidAppearance(WorldAccess world, BlockPos pos) {
        if (!(world instanceof ServerWorld serverWorld)) return;
        FluidState currentFluid = world.getFluidState(pos);
        if (currentFluid.isEmpty()) return;

        FluidOriginTracker fluidOriginTracker = WorldRegistry.get(serverWorld)
                .getTrackerManager()
                .getFluidOriginTracker();

        BlockPos above = pos.up();
        FluidState aboveFluid = world.getFluidState(above);

        if (!aboveFluid.isEmpty() && aboveFluid.getFluid() == currentFluid.getFluid()) {
            fluidOriginTracker.propagate(above, pos);
            return;
        }

        propagateFromNeighbors(world, pos);
    }

    @Unique
    private void propagateFromNeighbors(WorldAccess world, BlockPos pos) {
        if (!(world instanceof ServerWorld serverWorld)) return;
        FluidState currentFluid = world.getFluidState(pos);

        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.offset(dir);
            FluidState neighborFluid = world.getFluidState(neighborPos);

            if (!neighborFluid.isEmpty() && neighborFluid.getFluid() == currentFluid.getFluid()) {
                FluidOriginTracker fluidOriginTracker = WorldRegistry.get(serverWorld)
                        .getTrackerManager()
                        .getFluidOriginTracker();
                fluidOriginTracker.propagate(neighborPos, pos);
            }
        }
    }

    /* TODO: restriction on the reaction between water and lava
     * is under consideration for addition, as it breaks fluid physics.
     */
//    @Inject(method = "receiveNeighborFluids", at = {
//            @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)Z", ordinal = 0, shift = At.Shift.BEFORE),
//            @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)Z", ordinal = 1, shift = At.Shift.BEFORE)
//        },
//            cancellable = true)
//    private void preventBlockAppear(World world, BlockPos pos, BlockState state, CallbackInfoReturnable<Boolean> cir, @Local Direction direction) {
//        if (!(world instanceof ServerWorld serverWorld)) return;
//
//        BlockPos neighbourPos = pos.offset(direction.getOpposite());
//        UUID owner = FluidOriginTracker.getOwner(neighbourPos);
//
//        if (owner != null) {
//            ServerPlayerEntity player = serverWorld.getServer().getPlayerManager().getPlayer(owner);
//
//            if (!RegionPermissionChecker.isAllowed(player, Rule.SPREAD_LIQUID, pos)) {
//                PrivitMod.LOGGER.info("[FluidBlockMixin] BLOCKED reaction setBlockState at {} by {}", pos, player.getName().getString());
//
//                if (NotificationThrottler.allowSend(player, NotificationType.DENY_SPREAD_LIQUID)) {
//                    ServerPlayNetworking.send(player, new HudNotificationS2CPacket(
//                            NotificationType.DENY_SPREAD_LIQUID, Color.RED
//                    ));
//                }
//
//                cir.setReturnValue(true);
//            }
//        }
//    }
}