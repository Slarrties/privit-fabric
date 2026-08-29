package dev.slarrties.privit.server.region.protection.mixin.redstone;

import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.tracking.redstone.RedstoneSourceRegistry;
import dev.slarrties.privit.server.tracking.protection.RedstoneOriginTracker;

import net.minecraft.world.World;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.BlockState;
import net.minecraft.block.AbstractBlock;
import net.minecraft.server.world.ServerWorld;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractBlock.class)
public abstract class RedstoneBreakMixin {

    @Inject(method = "onStateReplaced", at = @At("HEAD"))
    private void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved, CallbackInfo ci) {
        if (!(world instanceof ServerWorld serverWorld) || moved) return;
        if (!RedstoneSourceRegistry.isSource(state, world, pos)) return;
        if (RedstoneSourceRegistry.isSource(newState, world, pos)) return;

        RedstoneOriginTracker redstoneOriginTracker = WorldRegistry.get(serverWorld)
                .getTrackerManager()
                .getRedstoneOriginTracker();
        redstoneOriginTracker.remove(pos);
    }
}