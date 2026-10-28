package dev.slarrties.privit.server.region.protection.mixin.redstone;

import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.tracking.protection.RedstoneOriginTracker;
import dev.slarrties.privit.server.tracking.redstone.RedstoneSourceRegistry;
import net.minecraft.world.level.block.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
// Mojmap: WorldAccess -> LevelAccessor
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;
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
//
//    @Inject(method = "getStateForNeighborUpdate", at = @At("HEAD"))
//    private void onHybridSourceNeighborUpdate(BlockState state, Direction direction, BlockState neighborState,
//                                              LevelAccessor level, BlockPos pos, BlockPos neighborPos,
//                                              CallbackInfoReturnable<BlockState> cir) {
//        // Mojmap: ServerWorld -> ServerLevel
//        if (!(level instanceof ServerLevel serverLevel)) return;
//        if (!RedstoneSourceRegistry.isSource(state, serverLevel, pos)) return;
//
//        RedstoneOriginTracker redstoneOriginTracker = WorldRegistry.get(serverLevel)
//                .getTrackerManager()
//                .getRedstoneOriginTracker();
//
//        if (redstoneOriginTracker.getResponsible(pos) == null) return;
//
//        UUID responsible = redstoneOriginTracker.getResponsible(neighborPos);
//        if (responsible != null) {
//            redstoneOriginTracker.record(pos, responsible);
//        }
//    }
}