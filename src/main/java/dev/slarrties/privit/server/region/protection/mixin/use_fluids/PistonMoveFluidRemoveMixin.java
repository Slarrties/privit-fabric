package dev.slarrties.privit.server.region.protection.mixin.use_fluids;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.tracking.protection.FluidOriginTracker;

import net.minecraft.world.World;
import net.minecraft.block.BlockState;
import net.minecraft.block.PistonBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@AssociatedRule(Rule.USE_FLUIDS)
@Mixin(PistonBlock.class)
public abstract class PistonMoveFluidRemoveMixin {

    @Inject(method = "onSyncedBlockEvent", at = @At("HEAD"))
    private void onPistonBlockEvent(BlockState state, World world, BlockPos pos, int type, int data,
            CallbackInfoReturnable<Boolean> cir) {
        if (!(world instanceof ServerWorld serverWorld)) return;

        Direction direction = state.get(PistonBlock.FACING);
        FluidOriginTracker fluidOriginTracker = WorldRegistry.get(serverWorld)
                .getTrackerManager()
                .getFluidOriginTracker();
        fluidOriginTracker.remove(pos);
        fluidOriginTracker.remove(pos.offset(direction));

        if (type == 1 || type == 2) {
            BlockPos behindPos = pos.offset(direction.getOpposite());
            fluidOriginTracker.remove(behindPos);
        }
    }
}