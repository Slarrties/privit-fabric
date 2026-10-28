package dev.slarrties.privit.server.region.protection.mixin.use_fire_starters;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;
import dev.slarrties.privit.server.tracking.context.BlockFallContext;
import dev.slarrties.privit.server.tracking.context.FireSpreadContext;
import dev.slarrties.privit.server.tracking.protection.FireOriginTracker;
import dev.slarrties.privit.server.tracking.protection.ExplosionOriginTracker;
import dev.slarrties.privit.server.tracking.protection.BlockFallOriginTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.UUID;

@AssociatedRule({Rule.USE_FIRE_STARTERS, Rule.CAUSE_BLOCK_FALL})
@Mixin(FireBlock.class)
public abstract class FireSpreadProtectionMixin {


    @Inject(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/FireBlock;checkBurnOut(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;ILnet/minecraft/util/RandomSource;I)V",
                    ordinal = 0,
                    shift = At.Shift.BEFORE
            )
    )
    private void beforeTrySpreadingEast(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        FireSpreadContext.push(pos, pos.east());
    }

    @Inject(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/FireBlock;checkBurnOut(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;ILnet/minecraft/util/RandomSource;I)V",
                    ordinal = 1,
                    shift = At.Shift.BEFORE
            )
    )
    private void beforeTrySpreadingWest(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        FireSpreadContext.push(pos, pos.west());
    }

    @Inject(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/FireBlock;checkBurnOut(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;ILnet/minecraft/util/RandomSource;I)V",
                    ordinal = 2,
                    shift = At.Shift.BEFORE
            )
    )
    private void beforeTrySpreadingDown(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        FireSpreadContext.push(pos, pos.below());
    }

    @Inject(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/FireBlock;checkBurnOut(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;ILnet/minecraft/util/RandomSource;I)V",
                    ordinal = 3,
                    shift = At.Shift.BEFORE
            )
    )
    private void beforeTrySpreadingUp(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        FireSpreadContext.push(pos, pos.above());
    }

    @Inject(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/FireBlock;checkBurnOut(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;ILnet/minecraft/util/RandomSource;I)V",
                    ordinal = 4,
                    shift = At.Shift.BEFORE
            )
    )
    private void beforeTrySpreadingNorth(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        FireSpreadContext.push(pos, pos.north());
    }

    @Inject(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/FireBlock;checkBurnOut(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;ILnet/minecraft/util/RandomSource;I)V",
                    ordinal = 5,
                    shift = At.Shift.BEFORE
            )
    )
    private void beforeTrySpreadingSouth(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        FireSpreadContext.push(pos, pos.south());
    }

    @Inject(method = "tick", at = {
            @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/FireBlock;checkBurnOut(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;ILnet/minecraft/util/RandomSource;I)V", ordinal = 0, shift = At.Shift.AFTER),
            @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/FireBlock;checkBurnOut(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;ILnet/minecraft/util/RandomSource;I)V", ordinal = 1, shift = At.Shift.AFTER),
            @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/FireBlock;checkBurnOut(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;ILnet/minecraft/util/RandomSource;I)V", ordinal = 2, shift = At.Shift.AFTER),
            @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/FireBlock;checkBurnOut(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;ILnet/minecraft/util/RandomSource;I)V", ordinal = 3, shift = At.Shift.AFTER),
            @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/FireBlock;checkBurnOut(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;ILnet/minecraft/util/RandomSource;I)V", ordinal = 4, shift = At.Shift.AFTER),
            @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/FireBlock;checkBurnOut(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;ILnet/minecraft/util/RandomSource;I)V", ordinal = 5, shift = At.Shift.AFTER),
    })
    private void afterLastTrySpreadingFire(CallbackInfo ci) {
        FireSpreadContext.pop();
    }

    @Inject(
            method = "checkBurnOut",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"
            ),
            cancellable = true
    )
    private void blockSetBlockState(Level level, BlockPos pos, int chance, RandomSource random, int age, CallbackInfo ci) {
        if (level instanceof ServerLevel serverLevel) {
            FireSpreadContext context = FireSpreadContext.getCurrent();
            if (context != null && context.getTargetPos().equals(pos)) {
                FireOriginTracker fireTracker = WorldRegistry.get(serverLevel).getTrackerManager().getFireOriginTracker();
                UUID responsible = fireTracker.getResponsible(context.getSourcePos());
                if (responsible != null) {
                    boolean allowed = RegionPermissionChecker.isAllowed(responsible, Rule.USE_FIRE_STARTERS, pos, serverLevel);
                    if (allowed) {
                        fireTracker.propagate(context.getSourcePos(), pos);
                        BlockFallOriginTracker blockFallTracker = WorldRegistry.get(serverLevel).getTrackerManager().getBlockFallOriginTracker();
                        blockFallTracker.record(pos, responsible);
                        BlockFallContext.push(responsible, pos);
                    } else {
                        this.sendDenyNotificationIfPossible(serverLevel, pos);
                        ci.cancel();
                    }
                }
            }
        }
    }

    @Inject(
            method = "checkBurnOut",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z",
                    shift = At.Shift.AFTER
            )
    )
    private void popFallAfterFireReplace(Level level, BlockPos pos, int chance, RandomSource random, int age, CallbackInfo ci) {
        BlockFallContext.pop();
    }

    @Inject(
            method = "checkBurnOut",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;removeBlock(Lnet/minecraft/core/BlockPos;Z)Z"
            ),
            cancellable = true
    )
    private void trackRemoveBlockInTrySpreadingFire(Level level, BlockPos pos, int chance, RandomSource random, int age, CallbackInfo ci) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        FireSpreadContext context = FireSpreadContext.getCurrent();
        if (context == null || !context.getTargetPos().equals(pos)) return;

        FireOriginTracker fireTracker = WorldRegistry.get(serverLevel).getTrackerManager().getFireOriginTracker();
        UUID responsible = fireTracker.getResponsible(context.getSourcePos());
        if (responsible != null) {
            if (!RegionPermissionChecker.isAllowed(responsible, Rule.USE_FIRE_STARTERS, pos, serverLevel)) {
                this.sendDenyNotificationIfPossible(serverLevel, context.getSourcePos());
                ci.cancel();
                return;
            }
            BlockFallOriginTracker blockFallTracker = WorldRegistry.get(serverLevel).getTrackerManager().getBlockFallOriginTracker();
            blockFallTracker.record(pos, responsible);
            BlockFallContext.push(responsible, pos);
        }
        fireTracker.remove(pos);
    }

    @Inject(
            method = "checkBurnOut",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;removeBlock(Lnet/minecraft/core/BlockPos;Z)Z",
                    shift = At.Shift.AFTER
            )
    )
    private void popFallAfterFireRemove(Level level, BlockPos pos, int chance, RandomSource random, int age, CallbackInfo ci) {
        BlockFallContext.pop();
    }

    @Inject(
            method = "checkBurnOut",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/TntBlock;prime(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)Z"
            ),
            cancellable = true
    )
    private void handleTntPrimingByFire(Level level, BlockPos pos, int chance, RandomSource random, int age, CallbackInfo ci) {
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) return;
        FireSpreadContext context = FireSpreadContext.getCurrent();
        if (context == null) return;

        ExplosionOriginTracker explosionTracker = WorldRegistry.get(serverLevel).getTrackerManager().getExplosionOriginTracker();
        FireOriginTracker fireTracker = WorldRegistry.get(serverLevel).getTrackerManager().getFireOriginTracker();
        UUID responsible = fireTracker.getResponsible(context.getSourcePos());

        ServerPlayer serverPlayer = serverLevel.getServer().getPlayerList().getPlayer(responsible);

        if (responsible != null) {
            BlockFallOriginTracker blockFallTracker = WorldRegistry.get(serverLevel).getTrackerManager().getBlockFallOriginTracker();
            blockFallTracker.record(pos, responsible);
            BlockFallContext.push(responsible, pos);

            PrimedTnt tntEntity = new PrimedTnt(level, (double) pos.getX() + 0.5, (double) pos.getY(), (double) pos.getZ() + 0.5, serverPlayer);
            level.addFreshEntity(tntEntity);

            BlockFallContext.pop();

            level.playSound(null, tntEntity.getX(), tntEntity.getY(), tntEntity.getZ(), SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 1.0F);
            explosionTracker.record(tntEntity, responsible);
            level.gameEvent(serverPlayer, GameEvent.PRIME_FUSE, pos);
        }
        ci.cancel();
    }

    @Inject(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerLevel;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z",
                    ordinal = 1
            ),
            cancellable = true
    )
    private void protectAndTrackBigSpread(BlockState state, ServerLevel serverLevel, BlockPos pos, RandomSource random, CallbackInfo ci, @Local BlockPos.MutableBlockPos testPos) {
        BlockPos newPos = testPos.immutable();
        FireOriginTracker fireTracker = WorldRegistry.get(serverLevel).getTrackerManager().getFireOriginTracker();
        UUID responsible = fireTracker.getResponsible(pos);

        if (responsible != null) {
            boolean allowed = RegionPermissionChecker.isAllowed(responsible, Rule.USE_FIRE_STARTERS, newPos, serverLevel);
            if (allowed) {
                fireTracker.propagate(pos, newPos);
                BlockFallOriginTracker blockFallTracker = WorldRegistry.get(serverLevel).getTrackerManager().getBlockFallOriginTracker();
                blockFallTracker.record(newPos, responsible);
                BlockFallContext.push(responsible, newPos);
            } else {
                this.sendDenyNotificationIfPossible(serverLevel, pos);
                ci.cancel();
            }
        }
    }

    @Inject(method = "tick", at = {
            @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;removeBlock(Lnet/minecraft/core/BlockPos;Z)Z", ordinal = 0),
            @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;removeBlock(Lnet/minecraft/core/BlockPos;Z)Z", ordinal = 1),
            @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;removeBlock(Lnet/minecraft/core/BlockPos;Z)Z", ordinal = 2),
            @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;removeBlock(Lnet/minecraft/core/BlockPos;Z)Z", ordinal = 3)
    })
    private void cleanupOnScheduledTickRemove0(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        FireOriginTracker fireTracker = WorldRegistry.get(level).getTrackerManager().getFireOriginTracker();
        fireTracker.remove(pos);
    }

    @Unique
    private void sendDenyNotificationIfPossible(ServerLevel serverLevel, BlockPos pos) {
        FireOriginTracker fireTracker = WorldRegistry.get(serverLevel).getTrackerManager().getFireOriginTracker();
        UUID responsible = fireTracker.getResponsible(pos);

        if (responsible != null) {
            ServerPlayer serverPlayer = serverLevel.getServer().getPlayerList().getPlayer(responsible);
            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_USE_FIRE_STARTER, Color.RED);
        }
    }
}