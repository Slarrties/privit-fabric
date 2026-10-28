package dev.slarrties.privit.server.region.protection.mixin.cause_explosions.tnt;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;
import dev.slarrties.privit.server.tracking.protection.ExplosionOriginTracker;
import dev.slarrties.privit.server.tracking.protection.BlockFallOriginTracker;
import dev.slarrties.privit.server.tracking.redstone.handler.RedstoneReceiverHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.UUID;

@AssociatedRule({Rule.CAUSE_EXPLOSIONS, Rule.CAUSE_BLOCK_FALL})
@Mixin(TntBlock.class)
public abstract class TntBlockExplosionMixin {

    @Inject(
            method = "wasExploded",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"
            ),
            locals = LocalCapture.CAPTURE_FAILSOFT
    )
    private void propagateResponsiblePlayerOnExplosion(ServerLevel level, BlockPos pos, Explosion explosion, CallbackInfo ci, PrimedTnt primed) {
        if (primed == null || explosion == null) return;

        ExplosionOriginTracker explosionOriginTracker = WorldRegistry.get(level)
                .getTrackerManager()
                .getExplosionOriginTracker();

        UUID responsible = explosionOriginTracker.getResponsible(explosion.getDirectSourceEntity());
        if (responsible == null && explosion.getIndirectSourceEntity() != null) {
            responsible = explosionOriginTracker.getResponsible(explosion.getIndirectSourceEntity());
        }

        if (responsible != null) {
            explosionOriginTracker.record(primed, responsible);
            markFallFromTnt(level, pos, responsible);
        }
    }


    @Inject(
            method = "prime(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/LivingEntity;)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"
            ),
            locals = LocalCapture.CAPTURE_FAILSOFT
    )
    private static void propagateResponsiblePlayerOnRedstoneActivation(Level level, BlockPos pos, LivingEntity source, CallbackInfoReturnable<Boolean> cir, ServerLevel serverLevel, PrimedTnt tnt) {
        if (level.isClientSide() || tnt == null) return;
        if (!(level instanceof ServerLevel serverLevel2)) return;

        UUID responsible = null;
        if (source instanceof ServerPlayer player) {
            responsible = player.getUUID();
        } else {
            responsible = RedstoneReceiverHandler.findResponsiblePlayer(serverLevel2, pos);
        }
        if (responsible == null) return;

        WorldRegistry.get(serverLevel2)
                .getTrackerManager()
                .getExplosionOriginTracker()
                .record(tnt, responsible);
        markFallFromTnt(serverLevel2, pos, responsible);
    }

    @Inject(
            method = "onProjectileHit",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/TntBlock;prime(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/LivingEntity;)Z"
            ),
            cancellable = true
    )
    private void preventFlameArrowIgnition(Level level, BlockState state, BlockHitResult hit, Projectile projectile, CallbackInfo ci) {
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) return;
        if (hit == null) return;

        UUID responsible = getResponsiblePlayer(projectile, level, hit.getBlockPos());
        if (responsible == null) return;
        if (!RegionPermissionChecker.isAllowed(responsible, Rule.CAUSE_EXPLOSIONS, hit.getBlockPos(), serverLevel)) {
            ServerPlayer serverPlayer = serverLevel.getServer()
                    .getPlayerList()
                    .getPlayer(responsible);
            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_CAUSE_EXPLOSION, Color.RED);
            ci.cancel();
        }
    }

    @Unique
    private UUID getResponsiblePlayer(Projectile projectile, Level level, BlockPos tntPos) {
        if (projectile.getOwner() instanceof ServerPlayer player) return player.getUUID();
        if (level instanceof ServerLevel serverLevel) return RedstoneReceiverHandler.findResponsiblePlayer(serverLevel, tntPos);

        return null;
    }

    @Unique
    private static void markFallFromTnt(ServerLevel serverLevel, BlockPos pos, UUID responsible) {
        if (responsible == null) return;
        BlockFallOriginTracker blockFallTracker = WorldRegistry.get(serverLevel)
                .getTrackerManager()
                .getBlockFallOriginTracker();
        blockFallTracker.record(pos, responsible);
    }
}