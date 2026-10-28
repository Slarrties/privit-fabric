package dev.slarrties.privit.server.region.protection.mixin.use_fluids;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.util.HeatSources;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.tracking.protection.CampfireOriginTracker;
import dev.slarrties.privit.server.tracking.protection.HeatSourceOriginTracker;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@AssociatedRule(Rule.USE_FLUIDS)
@Mixin(Block.class)
public abstract class HeatSourceStateReplacedMixin {

    @Inject(
            method = "updateOrDestroy(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;I)V",
            at = @At("HEAD")
    )
    private static void onHeatSourceReplaced(BlockState state, BlockState newState, LevelAccessor level, BlockPos pos, int updateFlags, CallbackInfo c) {
        if (state == newState) return;
        if (!(level instanceof ServerLevel serverLevel)) return;

        Block oldBlock = state.getBlock();
        if (!HeatSources.isHeatSource(oldBlock)) return;
        if (newState.getBlock() == oldBlock) return;

        HeatSourceOriginTracker heatSourceTracker = WorldRegistry.get(serverLevel)
                .getTrackerManager()
                .getHeatSourceOriginTracker();
        heatSourceTracker.remove(pos);

        if (oldBlock instanceof CampfireBlock) {
            CampfireOriginTracker campfireTracker = WorldRegistry.get(serverLevel)
                    .getTrackerManager()
                    .getCampfireOriginTracker();
            campfireTracker.remove(pos);
        }
    }
}