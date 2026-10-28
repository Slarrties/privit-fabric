package dev.slarrties.privit.server.region.protection.mixin.use_fluids;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.tracking.protection.FluidOriginTracker;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@AssociatedRule(Rule.USE_FLUIDS)
@Mixin(PistonMovingBlockEntity.class)
public abstract class PistonBlockEntityMixin {

    @Shadow
    private Direction direction;

    @Inject(method = "finalTick", at = @At("HEAD"))
    private void onPistonMovementFinished(CallbackInfo ci) {
        PistonMovingBlockEntity piston = (PistonMovingBlockEntity) (Object) this;
        Level level = piston.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) return;

        BlockPos pos = piston.getBlockPos();
        FluidOriginTracker fluidOriginTracker = WorldRegistry.get(serverLevel)
                .getTrackerManager()
                .getFluidOriginTracker();
        fluidOriginTracker.remove(pos);
        fluidOriginTracker.remove(pos.relative(direction));
    }
}