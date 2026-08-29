package dev.slarrties.privit.server.region.protection.mixin.use_fluids;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.util.HeatSources;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.tracking.protection.CampfireOriginTracker;
import dev.slarrties.privit.server.tracking.protection.HeatSourceOriginTracker;

import net.minecraft.block.*;
import net.minecraft.world.World;
import net.minecraft.util.math.BlockPos;
import net.minecraft.server.world.ServerWorld;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@AssociatedRule(Rule.USE_FLUIDS)
@Mixin(AbstractBlock.class)
public abstract class HeatSourceStateReplacedMixin {

    @Inject(method = "onStateReplaced", at = @At("HEAD"))
    private void onHeatSourceReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved, CallbackInfo c) {
        if (world.isClient || !(world instanceof ServerWorld serverWorld)) return;

        Block oldBlock = state.getBlock();
        if (!HeatSources.isHeatSource(oldBlock)) return;
        if (newState.getBlock() == oldBlock) return;

        HeatSourceOriginTracker heatSourceTracker = WorldRegistry.get(serverWorld)
                .getTrackerManager()
                .getHeatSourceOriginTracker();
        heatSourceTracker.remove(pos);

        if (oldBlock instanceof CampfireBlock) {
            CampfireOriginTracker campfireTracker = WorldRegistry.get(serverWorld)
                    .getTrackerManager()
                    .getCampfireOriginTracker();
            campfireTracker.remove(pos);
        }
    }
}