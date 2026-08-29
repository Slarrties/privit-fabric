package dev.slarrties.privit.server.region.protection.mixin.use_fluids;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.tracking.protection.FluidOriginTracker;

import net.minecraft.world.World;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.block.entity.PistonBlockEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@AssociatedRule(Rule.USE_FLUIDS)
@Mixin(PistonBlockEntity.class)
public abstract class PistonBlockEntityMixin {

    @Shadow
    private Direction facing;

    @Inject(method = "finish", at = @At("HEAD"))
    private void onPistonMovementFinished(CallbackInfo ci) {
        PistonBlockEntity piston = (PistonBlockEntity) (Object) this;
        World world = piston.getWorld();
        if (!(world instanceof ServerWorld serverWorld)) return;

        BlockPos pos = piston.getPos();
        FluidOriginTracker fluidOriginTracker = WorldRegistry.get(serverWorld)
                .getTrackerManager()
                .getFluidOriginTracker();
        fluidOriginTracker.remove(pos);
        fluidOriginTracker.remove(pos.offset(facing));
    }
}