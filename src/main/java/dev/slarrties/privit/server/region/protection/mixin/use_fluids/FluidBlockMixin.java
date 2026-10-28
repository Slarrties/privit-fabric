package dev.slarrties.privit.server.region.protection.mixin.use_fluids;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.tracking.protection.FluidOriginTracker;
import dev.slarrties.privit.server.tracking.context.FluidPlacementContext;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@AssociatedRule(Rule.USE_FLUIDS)
@Mixin(LiquidBlock.class)
public abstract class FluidBlockMixin {

    @Inject(method = "onPlace", at = @At("HEAD"))
    private void onAdded(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston, CallbackInfo ci) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        FluidPlacementContext context = FluidPlacementContext.getCurrent();
        if (context != null) {
            ServerPlayer placer = context.getPlayer();
            if (placer != null) {
                FluidOriginTracker fluidOriginTracker = WorldRegistry.get(serverLevel)
                        .getTrackerManager()
                        .getFluidOriginTracker();
                fluidOriginTracker.record(pos, placer.getUUID());
            }
        }
        FluidPlacementContext.pop();
    }

    @Inject(method = "pickupBlock", at = @At("HEAD"))
    private void onLiquidDrained(LivingEntity user, LevelAccessor level, BlockPos pos, BlockState state, CallbackInfoReturnable<ItemStack> cir) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        FluidOriginTracker fluidOriginTracker = WorldRegistry.get(serverLevel)
                .getTrackerManager()
                .getFluidOriginTracker();
        fluidOriginTracker.remove(pos);
    }

    @Inject(method = "updateShape", at = @At("RETURN"))
    private void onNeighborFluidUpdate(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random, CallbackInfoReturnable<BlockState> cir) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        BlockState newState = cir.getReturnValue();
        FluidState oldFluid = state.getFluidState();
        FluidState newFluid = newState.getFluidState();

        FluidOriginTracker fluidOriginTracker = WorldRegistry.get(serverLevel)
                .getTrackerManager()
                .getFluidOriginTracker();

        if (!oldFluid.isEmpty() && newFluid.isEmpty()) {
            fluidOriginTracker.remove(pos);
            return;
        }

        if (oldFluid.isEmpty() && !newFluid.isEmpty()) {
            handleFirstFluidAppearance(serverLevel, pos);
            return;
        }

        if (!oldFluid.isEmpty() && !newFluid.isEmpty()) {
            int oldLevel = oldFluid.getAmount();
            int newLevel = newFluid.getAmount();

            if (newLevel != oldLevel) {
                if (newLevel == 0) {
                    fluidOriginTracker.remove(pos);
                } else {
                    propagateFromNeighbors(serverLevel, pos);
                }
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
        propagateFromNeighbors(level, pos);
    }

    @Unique
    private void propagateFromNeighbors(ServerLevel level, BlockPos pos) {
        FluidState currentFluid = level.getFluidState(pos);

        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            FluidState neighborFluid = level.getFluidState(neighborPos);

            if (!neighborFluid.isEmpty() && neighborFluid.getType() == currentFluid.getType()) {
                FluidOriginTracker fluidOriginTracker = WorldRegistry.get(level)
                        .getTrackerManager()
                        .getFluidOriginTracker();
                fluidOriginTracker.propagate(neighborPos, pos);
            }
        }
    }
}