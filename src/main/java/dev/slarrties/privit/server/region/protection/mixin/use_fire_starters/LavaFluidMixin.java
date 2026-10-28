package dev.slarrties.privit.server.region.protection.mixin.use_fire_starters;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.tracking.protection.FireOriginTracker;
import dev.slarrties.privit.server.tracking.protection.FluidOriginTracker;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.LavaFluid;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@AssociatedRule(Rule.USE_FIRE_STARTERS)
@Mixin(LavaFluid.class)
public abstract class LavaFluidMixin {

    @Unique
    private static final ThreadLocal<BlockPos> CURRENT_LAVA_POS = new ThreadLocal<>();

    @Inject(method = "randomTick", at = @At("HEAD"))
    private void captureLavaPosition(ServerLevel level, BlockPos pos, FluidState fluidState, RandomSource random, CallbackInfo ci) {
        CURRENT_LAVA_POS.set(pos);
    }

    @Inject(method = "randomTick", at = @At("RETURN"))
    private void clearLavaPosition(ServerLevel level, BlockPos pos, FluidState fluidState, RandomSource random, CallbackInfo ci) {
        CURRENT_LAVA_POS.remove();
    }

    @WrapOperation(
            method = "randomTick",
            at = {
                    @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z", ordinal = 0),
                    @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z", ordinal = 1)
            }
    )
    private boolean wrapSetFireBlock(ServerLevel serverLevel, BlockPos firePos, BlockState fireState, Operation<Boolean> original) {
        if (fireState.is(Blocks.FIRE) || fireState.is(Blocks.SOUL_FIRE)) {
            BlockPos lavaPos = CURRENT_LAVA_POS.get();
            if (lavaPos != null) {
                FluidOriginTracker fluidOriginTracker = WorldRegistry.get(serverLevel)
                        .getTrackerManager()
                        .getFluidOriginTracker();
                UUID owner = fluidOriginTracker.getResponsible(lavaPos);

                if (owner != null) {
                    if (!RegionPermissionChecker.isAllowed(owner, Rule.USE_FIRE_STARTERS, firePos, serverLevel)) {
                        return false;
                    } else {
                        FireOriginTracker fireOriginTracker = WorldRegistry.get(serverLevel)
                                .getTrackerManager()
                                .getFireOriginTracker();
                        fireOriginTracker.record(firePos, owner);
                    }
                }
            }
        }
        return original.call(serverLevel, firePos, fireState);
    }
}