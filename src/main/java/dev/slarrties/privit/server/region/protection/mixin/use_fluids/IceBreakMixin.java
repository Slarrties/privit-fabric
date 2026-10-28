package dev.slarrties.privit.server.region.protection.mixin.use_fluids;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.tracking.protection.IceOriginTracker;
import dev.slarrties.privit.server.tracking.protection.FluidOriginTracker;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@AssociatedRule(Rule.USE_FLUIDS)
@Mixin(Block.class)
public abstract class IceBreakMixin {

    @Inject(
            method = "updateOrDestroy(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;I)V",
            at = @At("HEAD")
    )
    private static void onIceReplaced(BlockState state, BlockState newState, LevelAccessor level, BlockPos pos, int updateFlags, CallbackInfo ci) {
        if (state == newState) return;
        if (!(level instanceof ServerLevel serverLevel)) return;
        if (newState.is(Blocks.ICE)) return;

        IceOriginTracker iceOriginTracker = WorldRegistry.get(serverLevel)
                .getTrackerManager()
                .getIceOriginTracker();
        iceOriginTracker.remove(pos);

        if (!newState.getFluidState().isEmpty()) return;

        FluidOriginTracker fluidOriginTracker = WorldRegistry.get(serverLevel)
                .getTrackerManager()
                .getFluidOriginTracker();
        fluidOriginTracker.remove(pos);
    }
}