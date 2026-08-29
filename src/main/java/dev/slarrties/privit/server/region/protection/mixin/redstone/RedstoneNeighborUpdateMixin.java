package dev.slarrties.privit.server.region.protection.mixin.redstone;

import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.tracking.protection.RedstoneOriginTracker;
import dev.slarrties.privit.server.tracking.redstone.RedstoneSourceRegistry;

import net.minecraft.block.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.WorldAccess;
import net.minecraft.server.world.ServerWorld;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin({
        RepeaterBlock.class,
        ComparatorBlock.class,
        ObserverBlock.class
})
public abstract class RedstoneNeighborUpdateMixin {

    @Inject(method = "getStateForNeighborUpdate", at = @At("HEAD"))
    private void onHybridSourceNeighborUpdate(BlockState state, Direction direction, BlockState neighborState,
                                              WorldAccess world, BlockPos pos, BlockPos neighborPos,
                                              CallbackInfoReturnable<BlockState> cir) {
        if (!(world instanceof ServerWorld serverWorld)) return;
        if (!RedstoneSourceRegistry.isSource(state, serverWorld, pos)) return;

        RedstoneOriginTracker redstoneOriginTracker = WorldRegistry.get(serverWorld).getTrackerManager().getRedstoneOriginTracker();
        if(redstoneOriginTracker.getResponsible(pos) == null) return;

        UUID responsible = redstoneOriginTracker.getResponsible(neighborPos);
        if (responsible != null) redstoneOriginTracker.record(pos, responsible);
    }
}