package dev.slarrties.privit.server.region.protection.mixin.use_fire_starters;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.tracking.protection.FireOriginTracker;
import dev.slarrties.privit.server.tracking.protection.FluidOriginTracker;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;

import net.minecraft.world.World;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.LavaFluid;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.server.world.ServerWorld;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import java.util.UUID;

@AssociatedRule(Rule.USE_FIRE_STARTERS)
@Mixin(LavaFluid.class)
public abstract class LavaFluidMixin {

    @Unique
    private static final ThreadLocal<BlockPos> CURRENT_LAVA_POS = new ThreadLocal<>();

    @Inject(method = "onRandomTick", at = @At("HEAD"))
    private void captureLavaPosition(World world, BlockPos pos, FluidState state, Random random, CallbackInfo ci) {
        CURRENT_LAVA_POS.set(pos);
    }

    @Inject(method = "onRandomTick", at = @At("RETURN"))
    private void clearLavaPosition(World world, BlockPos pos, FluidState state, Random random, CallbackInfo ci) {
        CURRENT_LAVA_POS.remove();
    }

    @WrapOperation(
            method = "onRandomTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)Z"
            )
    )
    private boolean wrapSetFireBlock(World world, BlockPos firePos, BlockState fireState, Operation<Boolean> original) {
        BlockPos lavaPos = CURRENT_LAVA_POS.get();

        if (lavaPos != null && world instanceof ServerWorld serverWorld) {
            FluidOriginTracker fluidOriginTracker = WorldRegistry.get(serverWorld)
                    .getTrackerManager()
                    .getFluidOriginTracker();
            UUID owner = fluidOriginTracker.getOwner(lavaPos);

            if (owner != null) {
                if (!RegionPermissionChecker.isAllowed(owner, Rule.USE_FIRE_STARTERS, firePos, serverWorld)) {
                    return false;
                } else {
                    FireOriginTracker fireOriginTracker = WorldRegistry.get(serverWorld)
                            .getTrackerManager()
                            .getFireOriginTracker();
                    fireOriginTracker.record(firePos, owner);
                }
            }
        }

        return original.call(world, firePos, fireState);
    }
}