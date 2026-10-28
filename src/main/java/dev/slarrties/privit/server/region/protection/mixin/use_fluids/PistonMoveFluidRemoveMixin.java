package dev.slarrties.privit.server.region.protection.mixin.use_fluids;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.tracking.protection.FluidOriginTracker;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.piston.PistonBaseBlock;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@AssociatedRule(Rule.USE_FLUIDS)
@Mixin(PistonBaseBlock.class)
public abstract class PistonMoveFluidRemoveMixin {

    @Inject(method = "triggerEvent", at = @At("HEAD"))
    private void onPistonBlockEvent(BlockState state, Level level, BlockPos pos, int type, int data,
                                    CallbackInfoReturnable<Boolean> cir) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        Direction direction = state.getValue(PistonBaseBlock.FACING);
        FluidOriginTracker fluidOriginTracker = WorldRegistry.get(serverLevel)
                .getTrackerManager()
                .getFluidOriginTracker();
        fluidOriginTracker.remove(pos);
        fluidOriginTracker.remove(pos.relative(direction));

        if (type == 1 || type == 2) {
            BlockPos behindPos = pos.relative(direction.getOpposite());
            fluidOriginTracker.remove(behindPos);
        }
    }
}