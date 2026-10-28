package dev.slarrties.privit.server.region.protection.mixin.cause_block_fall;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.tracking.context.BlockFallContext;
import dev.slarrties.privit.server.tracking.protection.BlockFallOriginTracker;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@AssociatedRule(Rule.CAUSE_BLOCK_FALL)
@Mixin(BlockBehaviour.class)
public abstract class FallingBlockCascadeMixin {

    @Inject(method = "affectNeighborsAfterRemoval", at = @At("HEAD"))
    private void propagateFallOrigin(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston, CallbackInfo ci) {
        if (state.isAir()) return;

        UUID uuid = resolve(level, pos);
        if (uuid == null) return;

        BlockFallOriginTracker tracker = WorldRegistry.get(level)
                .getTrackerManager()
                .getBlockFallOriginTracker();
        tracker.record(pos, uuid);
        markAffectedNeighbors(level, pos, tracker, uuid);
    }

    @Unique
    private static void markAffectedNeighbors(ServerLevel level, BlockPos removed, BlockFallOriginTracker tracker, UUID uuid) {
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = removed.offset(dir.getUnitVec3i());
            BlockState neighbor = level.getBlockState(neighborPos);

            if (isGravityBlock(neighbor) && willFallIfSupportGone(level, neighborPos, removed, null))
                tracker.record(neighborPos, uuid);
            if (!isPopoffBlock(neighbor) || !dependsOn(neighbor, dir)) continue;

            tracker.record(neighborPos, uuid);
            for (Direction dir2 : Direction.values()) {
                BlockPos gravityPos = neighborPos.offset(dir2.getUnitVec3i());
                BlockState gravity = level.getBlockState(gravityPos);

                if (!isGravityBlock(gravity)) continue;
                if (!willFallIfSupportGone(level, gravityPos, removed, neighborPos)) continue;
                tracker.record(gravityPos, uuid);
            }
        }
    }

    @Unique
    private static boolean willFallIfSupportGone(ServerLevel level, BlockPos gravityPos,
                                                 BlockPos removed, @Nullable BlockPos alsoGone) {
        BlockPos below = gravityPos.below();
        if (below.equals(removed) || (alsoGone != null && below.equals(alsoGone))) return true;

        return FallingBlock.isFree(level.getBlockState(below));
    }

    @Unique
    private static boolean isGravityBlock(BlockState state) {
        Block block = state.getBlock();
        return block instanceof FallingBlock
                || block instanceof ScaffoldingBlock
                || block instanceof PointedDripstoneBlock;
    }

    @Unique
    private static boolean isPopoffBlock(BlockState state) {
        Block block = state.getBlock();
        return block instanceof TorchBlock
                || block instanceof WallTorchBlock
                || block instanceof LanternBlock
                || block instanceof ButtonBlock
                || block instanceof LeverBlock
                || block instanceof PressurePlateBlock
                || block instanceof RedStoneWireBlock
                || block instanceof CarpetBlock
                || block instanceof SnowLayerBlock
                || block instanceof TripWireHookBlock;
    }

    @Unique
    private static boolean dependsOn(BlockState popoff, Direction dirFromRemovedToNeighbor) {
        if (dirFromRemovedToNeighbor == Direction.UP) return true;
        if (popoff.hasProperty(WallTorchBlock.FACING)) return popoff.getValue(WallTorchBlock.FACING) == dirFromRemovedToNeighbor;

        return false;
    }

    @Unique
    private static UUID resolve(ServerLevel level, BlockPos pos) {
        BlockFallContext ctx = BlockFallContext.getCurrent();
        if (ctx != null && ctx.getResponsible() != null) return ctx.getResponsible();

        return WorldRegistry.get(level)
                .getTrackerManager()
                .getBlockFallOriginTracker()
                .getResponsible(pos);
    }
}