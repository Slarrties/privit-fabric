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

import net.minecraft.block.BlockState;
import net.minecraft.block.FireBlock;
import net.minecraft.entity.TntEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;

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

    @Inject(method = "scheduledTick", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/block/FireBlock;trySpreadingFire(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;ILnet/minecraft/util/math/random/Random;I)V",
            ordinal = 0, shift = At.Shift.BEFORE
    ))
    private void beforeTrySpreadingEast(BlockState state, ServerWorld world, BlockPos pos, Random random, CallbackInfo ci) {
        FireSpreadContext.push(pos, pos.east());
    }

    @Inject(method = "scheduledTick", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/block/FireBlock;trySpreadingFire(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;ILnet/minecraft/util/math/random/Random;I)V",
            ordinal = 1, shift = At.Shift.BEFORE
    ))
    private void beforeTrySpreadingWest(BlockState state, ServerWorld world, BlockPos pos, Random random, CallbackInfo ci) {
        FireSpreadContext.push(pos, pos.west());
    }

    @Inject(method = "scheduledTick", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/block/FireBlock;trySpreadingFire(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;ILnet/minecraft/util/math/random/Random;I)V",
            ordinal = 2, shift = At.Shift.BEFORE
    ))
    private void beforeTrySpreadingDown(BlockState state, ServerWorld world, BlockPos pos, Random random, CallbackInfo ci) {
        FireSpreadContext.push(pos, pos.down());
    }

    @Inject(method = "scheduledTick", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/block/FireBlock;trySpreadingFire(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;ILnet/minecraft/util/math/random/Random;I)V",
            ordinal = 3, shift = At.Shift.BEFORE
    ))
    private void beforeTrySpreadingUp(BlockState state, ServerWorld world, BlockPos pos, Random random, CallbackInfo ci) {
        FireSpreadContext.push(pos, pos.up());
    }

    @Inject(method = "scheduledTick", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/block/FireBlock;trySpreadingFire(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;ILnet/minecraft/util/math/random/Random;I)V",
            ordinal = 4, shift = At.Shift.BEFORE
    ))
    private void beforeTrySpreadingNorth(BlockState state, ServerWorld world, BlockPos pos, Random random, CallbackInfo ci) {
        FireSpreadContext.push(pos, pos.north());
    }

    @Inject(method = "scheduledTick", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/block/FireBlock;trySpreadingFire(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;ILnet/minecraft/util/math/random/Random;I)V",
            ordinal = 5, shift = At.Shift.BEFORE
    ))
    private void beforeTrySpreadingSouth(BlockState state, ServerWorld world, BlockPos pos, Random random, CallbackInfo ci) {
        FireSpreadContext.push(pos, pos.south());
    }

    @Inject(method = "scheduledTick", at = {
                    @At(value = "INVOKE", target = "Lnet/minecraft/block/FireBlock;trySpreadingFire(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;ILnet/minecraft/util/math/random/Random;I)V", ordinal = 0, shift = At.Shift.AFTER),
                    @At(value = "INVOKE", target = "Lnet/minecraft/block/FireBlock;trySpreadingFire(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;ILnet/minecraft/util/math/random/Random;I)V", ordinal = 1, shift = At.Shift.AFTER),
                    @At(value = "INVOKE", target = "Lnet/minecraft/block/FireBlock;trySpreadingFire(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;ILnet/minecraft/util/math/random/Random;I)V", ordinal = 2, shift = At.Shift.AFTER),
                    @At(value = "INVOKE", target = "Lnet/minecraft/block/FireBlock;trySpreadingFire(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;ILnet/minecraft/util/math/random/Random;I)V", ordinal = 3, shift = At.Shift.AFTER),
                    @At(value = "INVOKE", target = "Lnet/minecraft/block/FireBlock;trySpreadingFire(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;ILnet/minecraft/util/math/random/Random;I)V", ordinal = 4, shift = At.Shift.AFTER),
                    @At(value = "INVOKE", target = "Lnet/minecraft/block/FireBlock;trySpreadingFire(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;ILnet/minecraft/util/math/random/Random;I)V", ordinal = 5, shift = At.Shift.AFTER),
            }
    )
    private void afterLastTrySpreadingFire(CallbackInfo ci) {
        FireSpreadContext.pop();
    }

    @Inject(method = "trySpreadingFire", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"
    ), cancellable = true)
    private void blockSetBlockState(World world, BlockPos pos, int spreadFactor, Random random, int currentAge, CallbackInfo ci) {
        if(world instanceof ServerWorld serverWorld) {
            FireSpreadContext context = FireSpreadContext.getCurrent();

            if(context.getTargetPos().equals(pos)) {
                FireOriginTracker fireTracker = WorldRegistry.get(serverWorld)
                        .getTrackerManager()
                        .getFireOriginTracker();
                UUID responsible = fireTracker.getResponsible(context.getSourcePos());

                if(responsible != null) {
                    boolean allowed = RegionPermissionChecker.isAllowed(responsible, Rule.USE_FIRE_STARTERS, pos, serverWorld);

                    if (allowed) {
                        fireTracker.propagate(context.getSourcePos(), pos);

                        BlockFallOriginTracker blockFallTracker = WorldRegistry.get(serverWorld)
                                .getTrackerManager()
                                .getBlockFallOriginTracker();
                        blockFallTracker.record(pos, responsible);
                        BlockFallContext.push(responsible, pos);
                    } else {
                        this.sendDenyNotificationIfPossible(serverWorld, pos);
                        ci.cancel();
                    }
                }
            }
        }
    }

    @Inject(
            method = "trySpreadingFire",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z",
                    shift = At.Shift.AFTER
            )
    )
    private void popFallAfterFireReplace(World world, BlockPos pos, int spreadFactor,
                                         Random random, int currentAge, CallbackInfo ci) {
        BlockFallContext.pop();
    }

    @Inject(
            method = "trySpreadingFire",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;removeBlock(Lnet/minecraft/util/math/BlockPos;Z)Z"
            ),
            cancellable = true
    )
    private void trackRemoveBlockInTrySpreadingFire(World world, BlockPos pos, int spreadFactor,
                                                    Random random, int currentAge, CallbackInfo ci) {
        if (!(world instanceof ServerWorld serverWorld)) return;

        FireSpreadContext context = FireSpreadContext.getCurrent();
        if (context == null || !context.getTargetPos().equals(pos)) return;

        FireOriginTracker fireTracker = WorldRegistry.get(serverWorld)
                .getTrackerManager()
                .getFireOriginTracker();
        UUID responsible = fireTracker.getResponsible(context.getSourcePos());

        if (responsible != null) {
            if (!RegionPermissionChecker.isAllowed(responsible, Rule.USE_FIRE_STARTERS, pos, serverWorld)) {
                this.sendDenyNotificationIfPossible(serverWorld, context.getSourcePos());
                ci.cancel();
                return;
            }

            BlockFallOriginTracker blockFallTracker = WorldRegistry.get(serverWorld)
                    .getTrackerManager()
                    .getBlockFallOriginTracker();
            blockFallTracker.record(pos, responsible);
            BlockFallContext.push(responsible, pos);
        }

        fireTracker.remove(pos);
    }

    @Inject(
            method = "trySpreadingFire",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;removeBlock(Lnet/minecraft/util/math/BlockPos;Z)Z",
                    shift = At.Shift.AFTER
            )
    )
    private void popFallAfterFireRemove(World world, BlockPos pos, int spreadFactor,
                                        Random random, int currentAge, CallbackInfo ci) {
        BlockFallContext.pop();
    }

    @Inject(
            method = "trySpreadingFire",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/block/TntBlock;primeTnt(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;)V"
            ),
            cancellable = true
    )
    private void handleTntPrimingByFire(World world, BlockPos pos, int spreadFactor, Random random, int currentAge, CallbackInfo ci) {
        if (world.isClient || !(world instanceof ServerWorld serverWorld)) return;

        FireSpreadContext context = FireSpreadContext.getCurrent();
        if (context == null) return;

        ExplosionOriginTracker explosionTracker = WorldRegistry.get(serverWorld)
                .getTrackerManager()
                .getExplosionOriginTracker();
        FireOriginTracker fireTracker = WorldRegistry.get(serverWorld)
                .getTrackerManager()
                .getFireOriginTracker();
        UUID responsible = fireTracker.getResponsible(context.getSourcePos());
        ServerPlayerEntity serverPlayer = serverWorld.getServer()
                .getPlayerManager()
                .getPlayer(responsible);

        if (responsible != null) {
            BlockFallOriginTracker blockFallTracker = WorldRegistry.get(serverWorld)
                    .getTrackerManager()
                    .getBlockFallOriginTracker();
            blockFallTracker.record(pos, responsible);
            BlockFallContext.push(responsible, pos);

            TntEntity tntEntity = new TntEntity(
                    world,
                    (double)pos.getX() + 0.5,
                    (double)pos.getY(),
                    (double)pos.getZ() + 0.5,
                    serverPlayer
            );
            world.spawnEntity(tntEntity);

            BlockFallContext.pop();

            world.playSound(
                    null,
                    tntEntity.getX(), tntEntity.getY(), tntEntity.getZ(),
                    SoundEvents.ENTITY_TNT_PRIMED,
                    SoundCategory.BLOCKS,
                    1.0F, 1.0F
            );
            explosionTracker.record(tntEntity, responsible);
            world.emitGameEvent(serverPlayer, GameEvent.PRIME_FUSE, pos);
        }

        ci.cancel();
    }

    @Inject(method = "scheduledTick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/world/ServerWorld;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z",
            ordinal = 1), cancellable = true)
    private void protectAndTrackBigSpread(BlockState state, ServerWorld serverWorld, BlockPos pos,
                                          Random random, CallbackInfo ci, @Local BlockPos.Mutable mutable) {
        BlockPos newPos = mutable.toImmutable();
        FireOriginTracker fireTracker = WorldRegistry.get(serverWorld)
                .getTrackerManager()
                .getFireOriginTracker();
        UUID responsible = fireTracker.getResponsible(pos);

        if(responsible != null) {
            boolean allowed = RegionPermissionChecker.isAllowed(responsible, Rule.USE_FIRE_STARTERS, newPos, serverWorld);

            if(allowed) {
                fireTracker.propagate(pos, newPos);
                BlockFallOriginTracker blockFallTracker = WorldRegistry.get(serverWorld)
                        .getTrackerManager()
                        .getBlockFallOriginTracker();
                blockFallTracker.record(newPos, responsible);
                BlockFallContext.push(responsible, newPos);
            } else {
                this.sendDenyNotificationIfPossible(serverWorld, pos);
                ci.cancel();
            }
        }
    }

    @Inject(method = "scheduledTick", at = {
            @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerWorld;removeBlock(Lnet/minecraft/util/math/BlockPos;Z)Z", ordinal = 0),
            @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerWorld;removeBlock(Lnet/minecraft/util/math/BlockPos;Z)Z", ordinal = 1),
            @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerWorld;removeBlock(Lnet/minecraft/util/math/BlockPos;Z)Z", ordinal = 2),
            @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerWorld;removeBlock(Lnet/minecraft/util/math/BlockPos;Z)Z", ordinal = 3)
    })
    private void cleanupOnScheduledTickRemove0(BlockState state, ServerWorld world, BlockPos pos, Random random, CallbackInfo ci) {
        FireOriginTracker fireTracker = WorldRegistry.get(world)
                .getTrackerManager()
                .getFireOriginTracker();
        fireTracker.remove(pos);
    }

    @Unique
    private void sendDenyNotificationIfPossible(ServerWorld serverWorld, BlockPos pos) {
        FireOriginTracker fireTracker = WorldRegistry.get(serverWorld)
                .getTrackerManager()
                .getFireOriginTracker();
        UUID responsible = fireTracker.getResponsible(pos);
        ServerPlayerEntity serverPlayer = serverWorld.getServer()
                .getPlayerManager()
                .getPlayer(responsible);
        PlayerNotification.trySend(serverPlayer, NotificationType.DENY_USE_FIRE_STARTER, Color.RED);
    }
}