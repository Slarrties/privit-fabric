package dev.slarrties.privit.server.region.protection.mixin.use_fluids;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.tracking.protection.IceOriginTracker;
import dev.slarrties.privit.server.tracking.protection.FluidOriginTracker;

import net.minecraft.world.World;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.AbstractBlock;
import net.minecraft.server.world.ServerWorld;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@AssociatedRule(Rule.USE_FLUIDS)
@Mixin(AbstractBlock.class)
public abstract class IceBreakMixin {

    @Inject(method = "onStateReplaced", at = @At("HEAD"))
    private void onIceReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved, CallbackInfo ci) {
        if (!(world instanceof ServerWorld serverWorld)) return;
        if (newState.isOf(Blocks.ICE)) return;

        IceOriginTracker iceOriginTracker = WorldRegistry.get(serverWorld)
                .getTrackerManager()
                .getIceOriginTracker();
        iceOriginTracker.remove(pos);

        if (!newState.getFluidState().isEmpty()) return;

        FluidOriginTracker fluidOriginTracker = WorldRegistry.get(serverWorld)
                .getTrackerManager()
                .getFluidOriginTracker();
        fluidOriginTracker.remove(pos);
    }
}