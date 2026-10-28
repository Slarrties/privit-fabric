package dev.slarrties.privit.server.region.protection.mixin.redstone;

import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.tracking.redstone.RedstoneSourceRegistry;
import dev.slarrties.privit.server.tracking.protection.RedstoneOriginTracker;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Block.class)
public abstract class RedstoneBreakMixin {

    @Inject(
            method = "updateOrDestroy(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;I)V",
            at = @At("HEAD")
    )
    private static void onBlockStateChange(BlockState blockState, BlockState newState, LevelAccessor level, BlockPos blockPos, int updateFlags, CallbackInfo ci) {
        if (blockState == newState) return;
        if (!(level instanceof ServerLevel serverLevel)) return;
        if (!RedstoneSourceRegistry.isSource(blockState, serverLevel, blockPos)) return;
        if (RedstoneSourceRegistry.isSource(newState, serverLevel, blockPos)) return;

        RedstoneOriginTracker redstoneOriginTracker = WorldRegistry.get(serverLevel)
                .getTrackerManager()
                .getRedstoneOriginTracker();
        redstoneOriginTracker.remove(blockPos);
    }
}