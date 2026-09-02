package dev.slarrties.privit.server.region.protection.mixin.cause_block_fall;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.tracking.context.BlockFallContext;
import dev.slarrties.privit.server.tracking.protection.BlockFallOriginTracker;

import net.minecraft.block.*;
import net.minecraft.world.World;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.server.world.ServerWorld;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@AssociatedRule(Rule.CAUSE_BLOCK_FALL)
@Mixin(AbstractBlock.class)
public abstract class FallingBlockCascadeMixin {

    @Inject(method = "onStateReplaced", at = @At("HEAD"))
    private void propagateFallOrigin(BlockState state, World world, BlockPos pos,
                                     BlockState newState, boolean moved, CallbackInfo ci) {
        if (world.isClient() || !(world instanceof ServerWorld serverWorld)) return;
        if (state.isAir()) return;
        if (newState.getBlock() == state.getBlock()) return;

        UUID uuid = resolve(serverWorld, pos);
        if (uuid == null) return;

        BlockFallOriginTracker tracker = WorldRegistry.get(serverWorld)
                .getTrackerManager()
                .getBlockFallOriginTracker();
        tracker.record(pos, uuid);
        markAffectedNeighbors(serverWorld, pos, tracker, uuid);
    }

    @Unique
    private static void markAffectedNeighbors(ServerWorld world, BlockPos removed,
                                              BlockFallOriginTracker tracker, UUID uuid) {
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = removed.offset(dir);
            BlockState neighbor = world.getBlockState(neighborPos);

            if (isGravityBlock(neighbor) && willFallIfSupportGone(world, neighborPos, removed, null)) {
                tracker.record(neighborPos, uuid);
            }

            if (!isPopoffBlock(neighbor) || !dependsOn(neighbor, dir)) continue;

            tracker.record(neighborPos, uuid);

            for (Direction dir2 : Direction.values()) {
                BlockPos gravityPos = neighborPos.offset(dir2);
                BlockState gravity = world.getBlockState(gravityPos);
                if (!isGravityBlock(gravity)) continue;
                if (!willFallIfSupportGone(world, gravityPos, removed, neighborPos)) continue;
                tracker.record(gravityPos, uuid);
            }
        }
    }

    @Unique
    private static boolean willFallIfSupportGone(World world, BlockPos gravityPos,
                                                 BlockPos removed, @Nullable BlockPos alsoGone) {
        BlockPos below = gravityPos.down();
        if (below.equals(removed) || (alsoGone != null && below.equals(alsoGone))) {
            return true;
        }

        return FallingBlock.canFallThrough(world.getBlockState(below));
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
        return block instanceof AbstractTorchBlock
                || block instanceof WallTorchBlock
                || block instanceof LanternBlock
                || block instanceof ButtonBlock
                || block instanceof LeverBlock
                || block instanceof PressurePlateBlock
                || block instanceof RedstoneWireBlock
                || block instanceof CarpetBlock
                || block instanceof SnowBlock
                || block instanceof TripwireHookBlock;
    }

    @Unique
    private static boolean dependsOn(BlockState popoff, Direction dirFromRemovedToNeighbor) {
        if (dirFromRemovedToNeighbor == Direction.UP) return true;
        if (popoff.contains(WallTorchBlock.FACING)) {
            return popoff.get(WallTorchBlock.FACING) == dirFromRemovedToNeighbor;
        }

        return false;
    }

    @Unique
    private static UUID resolve(ServerWorld world, BlockPos pos) {
        BlockFallContext ctx = BlockFallContext.getCurrent();
        if (ctx != null && ctx.getResponsible() != null) {
            return ctx.getResponsible();
        }

        return WorldRegistry.get(world)
                .getTrackerManager()
                .getBlockFallOriginTracker()
                .getResponsible(pos);
    }
}