package dev.slarrties.privit.server.region.protection.mixin.use_sponges;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;
import dev.slarrties.privit.server.tracking.protection.FluidOriginTracker;
import dev.slarrties.privit.server.tracking.context.SpongePlacementContext;

import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.TraversalNodeStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SpongeBlock;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import java.util.function.Function;
import java.util.function.Consumer;
import java.util.function.BiConsumer;

@AssociatedRule({Rule.USE_SPONGES, Rule.USE_FLUIDS})
@Mixin(SpongeBlock.class)
public abstract class SpongeAbsorbMixin {

    @Unique private static final ThreadLocal<ServerPlayer> CURRENT_PLAYER = new ThreadLocal<>();
    @Unique private static final ThreadLocal<Boolean> HAS_NOTIFIED = ThreadLocal.withInitial(() -> false);

    @Inject(method = "tryAbsorbWater", at = @At("HEAD"))
    private void onTryAbsorbHead(Level level, BlockPos pos, CallbackInfo ci) {
        SpongePlacementContext context = SpongePlacementContext.getCurrent();
        if (context != null && context.getPlayer() != null && context.getPos().equals(pos)) {
            CURRENT_PLAYER.set(context.getPlayer());
        }
        HAS_NOTIFIED.set(false);
    }

    @Inject(method = "tryAbsorbWater", at = @At("RETURN"))
    private void onTryAbsorbReturn(Level level, BlockPos pos, CallbackInfo ci) {
        if (HAS_NOTIFIED.get() && CURRENT_PLAYER.get() != null) {
            PlayerNotification.trySend(CURRENT_PLAYER.get(), NotificationType.DENY_USE_SPONGE, Color.RED);
        }
        CURRENT_PLAYER.remove();
        HAS_NOTIFIED.remove();
    }

    @WrapOperation(
            method = "removeWaterBreadthFirstSearch",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/core/BlockPos;breadthFirstTraversal(Lnet/minecraft/core/BlockPos;IILjava/util/function/BiConsumer;Ljava/util/function/Function;)I"
            )
    )
    private int wrapBreadthFirstTraversal(
            BlockPos startPos, int maxDepth, int maxCount,
            BiConsumer<BlockPos, Consumer<BlockPos>> neighborConsumer,
            Function<BlockPos, TraversalNodeStatus> originalStatusFunction,
            Operation<Integer> original
    ) {
        Function<BlockPos, TraversalNodeStatus> wrappedStatusFunction = (pos) -> {
            if (isProtected(pos)) {
                HAS_NOTIFIED.set(true);
                return TraversalNodeStatus.SKIP;
            }

            TraversalNodeStatus status = originalStatusFunction.apply(pos);

            if (status == TraversalNodeStatus.ACCEPT) {
                removeFluidTracker(pos);
            }

            return status;
        };

        return original.call(startPos, maxDepth, maxCount, neighborConsumer, wrappedStatusFunction);
    }

    @Unique
    private static boolean isProtected(BlockPos pos) {
        ServerPlayer player = CURRENT_PLAYER.get();
        if (player == null) return false;
        if (!(player.level() instanceof ServerLevel serverLevel)) return false;

        return !RegionPermissionChecker.isAllowed(player.getUUID(), Rule.USE_SPONGES, pos, serverLevel);
    }

    @Unique
    private static void removeFluidTracker(BlockPos pos) {
        if (CURRENT_PLAYER.get() != null && CURRENT_PLAYER.get().level() instanceof ServerLevel serverLevel) {
            FluidOriginTracker tracker = WorldRegistry.get(serverLevel)
                    .getTrackerManager()
                    .getFluidOriginTracker();
            tracker.remove(pos);
        }
    }
}