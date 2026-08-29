package dev.slarrties.privit.server.region.protection.mixin.cause_explosions.tnt;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;
import dev.slarrties.privit.server.tracking.protection.ExplosionOriginTracker;
import dev.slarrties.privit.server.tracking.redstone.handler.RedstoneReceiverHandler;

import net.minecraft.block.TntBlock;
import net.minecraft.block.BlockState;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.UUID;

@AssociatedRule(Rule.CAUSE_EXPLOSIONS)
@Mixin(TntBlock.class)
public abstract class TntBlockExplosionMixin {

    @Inject(
            method = "onDestroyedByExplosion",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;spawnEntity(Lnet/minecraft/entity/Entity;)Z"
            ),
            locals = LocalCapture.CAPTURE_FAILSOFT
    )
    private void propagateResponsiblePlayerOnExplosion(World world, BlockPos pos, Explosion explosion, CallbackInfo ci, TntEntity tntEntity) {
        if (world.isClient || tntEntity == null || explosion == null) return;
        if(world instanceof ServerWorld serverWorld) {
            ExplosionOriginTracker explosionOriginTracker = WorldRegistry.get(serverWorld).getTrackerManager().getExplosionOriginTracker();
            UUID responsible = explosionOriginTracker.getResponsiblePlayer(explosion.getEntity());

            if (responsible == null) {
                if (explosion.getCausingEntity() != null) {
                    responsible = explosionOriginTracker.getResponsiblePlayer(explosion.getCausingEntity());
                }
            }

            if (responsible != null) {
                explosionOriginTracker.record(tntEntity, responsible);
            }
        }
    }

    @Inject(
            method = "primeTnt(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/LivingEntity;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;spawnEntity(Lnet/minecraft/entity/Entity;)Z"
            ),
            locals = LocalCapture.CAPTURE_FAILSOFT
    )
    private static void propagateResponsiblePlayerOnRedstoneActivation(World world, BlockPos pos,
            @Nullable LivingEntity igniter, CallbackInfo ci, TntEntity tntEntity) {
        if (world.isClient || tntEntity == null || igniter != null) return;
        if (world instanceof ServerWorld serverWorld) {
            UUID responsible = RedstoneReceiverHandler.findResponsiblePlayer(serverWorld, pos);

            if (responsible != null) {
                WorldRegistry.get(serverWorld)
                        .getTrackerManager()
                        .getExplosionOriginTracker()
                        .record(tntEntity, responsible);
            }
        }
    }

    @Inject(
            method = "onProjectileHit",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/block/TntBlock;primeTnt(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/LivingEntity;)V"
            ),
            cancellable = true
    )
    private void preventFlameArrowIgnition(World world, BlockState state, BlockHitResult hit, ProjectileEntity projectile, CallbackInfo ci) {
        if (world.isClient || !(world instanceof ServerWorld serverWorld)) return;
        if (hit == null) return;

        UUID responsible = getResponsiblePlayer(projectile, world, hit.getBlockPos());
        if (responsible == null) return;
        if (!RegionPermissionChecker.isAllowed(responsible, Rule.CAUSE_EXPLOSIONS, hit.getBlockPos(), serverWorld)) {
            ServerPlayerEntity serverPlayer = serverWorld.getServer()
                    .getPlayerManager()
                    .getPlayer(responsible);
            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_CAUSE_EXPLOSION, Color.RED);
            ci.cancel();
        }
    }

    @Unique
    private UUID getResponsiblePlayer(ProjectileEntity projectile, World world, BlockPos tntPos) {
        if (projectile.getOwner() instanceof ServerPlayerEntity player) {
            return player.getUuid();
        }

        if (world instanceof ServerWorld serverWorld) {
            return RedstoneReceiverHandler.findResponsiblePlayer(serverWorld, tntPos);
        }

        return null;
    }
}