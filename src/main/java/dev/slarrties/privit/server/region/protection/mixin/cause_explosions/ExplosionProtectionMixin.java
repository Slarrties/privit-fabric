package dev.slarrties.privit.server.region.protection.mixin.cause_explosions;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;
import dev.slarrties.privit.server.tracking.context.BlockFallContext;
import dev.slarrties.privit.server.tracking.protection.ExplosionOriginTracker;
import dev.slarrties.privit.server.tracking.protection.BlockFallOriginTracker;

import net.minecraft.entity.Entity;
import net.minecraft.entity.Ownable;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.projectile.WitherSkullEntity;
import net.minecraft.entity.projectile.AbstractWindChargeEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@AssociatedRule({
        Rule.CAUSE_EXPLOSIONS,
        Rule.THROW_WIND_CHARGES,
        Rule.CAUSE_BLOCK_FALL
})
@Mixin(Explosion.class)
public abstract class ExplosionProtectionMixin {

    @Shadow @Final private World world;

    @Unique
    private Explosion getExplosion() {
        return (Explosion) (Object) this;
    }

    @WrapOperation(
            method = "collectBlocksAndDamageEntities",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/Entity;damage(Lnet/minecraft/entity/damage/DamageSource;F)Z"
            )
    )
    private boolean preventExplosionDamage(Entity entity, DamageSource source, float amount, Operation<Boolean> original) {
        if (!(this.world instanceof ServerWorld serverWorld)) {
            return original.call(entity, source, amount);
        }

        UUID responsible = getResponsiblePlayer(serverWorld);
        if (responsible == null) {
            return original.call(entity, source, amount);
        }

        Rule rule = getRuleToCheck();
        if (RegionPermissionChecker.isAllowed(responsible, rule, entity.getBlockPos(), serverWorld)) {
            return original.call(entity, source, amount);
        }

        sendDenyNotification(responsible, rule, serverWorld);
        return false;
    }

    @WrapOperation(
            method = "collectBlocksAndDamageEntities",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/Entity;setVelocity(Lnet/minecraft/util/math/Vec3d;)V"
            )
    )
    private void preventExplosionKnockback(Entity entity, Vec3d velocity, Operation<Void> original) {
        if (!(this.world instanceof ServerWorld serverWorld)) {
            original.call(entity, velocity);
            return;
        }

        UUID responsible = getResponsiblePlayer(serverWorld);
        if (responsible == null) {
            original.call(entity, velocity);
            return;
        }

        Rule rule = getRuleToCheck();
        if (RegionPermissionChecker.isAllowed(responsible, rule, entity.getBlockPos(), serverWorld)) {
            original.call(entity, velocity);
        }
    }

    @Inject(method = "collectBlocksAndDamageEntities", at = @At("RETURN"))
    private void filterProtectedBlocks(CallbackInfo ci) {
        if (!(this.world instanceof ServerWorld serverWorld)) return;

        UUID responsible = getResponsiblePlayer(serverWorld);
        if (responsible == null) return;

        Rule rule = getRuleToCheck();

        getExplosion().getAffectedBlocks().removeIf(pos ->
                !RegionPermissionChecker.isAllowed(responsible, rule, pos, serverWorld)
        );
    }

    @Inject(method = "affectWorld", at = @At("HEAD"))
    private void markFallFromExplosion(boolean particles, CallbackInfo ci) {
        if (!(this.world instanceof ServerWorld serverWorld)) return;

        UUID responsible = getResponsiblePlayer(serverWorld);
        if (responsible == null) return;

        BlockFallContext.push(responsible, BlockPos.ofFloored(getExplosion().getPosition()));

        for (BlockPos pos : getExplosion().getAffectedBlocks()) {
            BlockFallOriginTracker blockFallTracker = WorldRegistry.get(serverWorld)
                    .getTrackerManager()
                    .getBlockFallOriginTracker();
            blockFallTracker.record(pos, responsible);
        }
    }

    @Inject(method = "affectWorld", at = @At("RETURN"))
    private void popFallFromExplosion(boolean particles, CallbackInfo ci) {
        BlockFallContext.pop();
    }

    @Unique
    private UUID getResponsiblePlayer(ServerWorld serverWorld) {
        Entity exploder = getExplosion().getEntity();
        if (exploder == null) return null;

        ExplosionOriginTracker tracker = WorldRegistry.get(serverWorld)
                .getTrackerManager()
                .getExplosionOriginTracker();

        UUID responsible = tracker.getResponsiblePlayer(exploder);
        if (responsible != null) return responsible;

        if (exploder instanceof ServerPlayerEntity player) {
            return player.getUuid();
        }

        if (exploder instanceof WitherSkullEntity skull) {
            Entity owner = skull.getOwner();
            if (owner instanceof WitherEntity wither) {
                responsible = tracker.getResponsiblePlayer(wither);
                if (responsible != null) return responsible;
            }
        }

        if (exploder instanceof Ownable ownable) {
            Entity owner = ownable.getOwner();
            if (owner instanceof ServerPlayerEntity player) {
                return player.getUuid();
            }
            if (owner != null) {
                responsible = tracker.getResponsiblePlayer(owner);
                if (responsible != null) return responsible;
            }
        }

        return null;
    }

    @Unique
    private Rule getRuleToCheck() {
        Entity exploder = getExplosion().getEntity();
        return (exploder instanceof AbstractWindChargeEntity)
                ? Rule.THROW_WIND_CHARGES
                : Rule.CAUSE_EXPLOSIONS;
    }

    @Unique
    private void sendDenyNotification(UUID responsible, Rule rule, ServerWorld serverWorld) {
        NotificationType type = (rule == Rule.THROW_WIND_CHARGES)
                ? NotificationType.DENY_THROW_WIND_CHARGE
                : NotificationType.DENY_CAUSE_EXPLOSION;

        ServerPlayerEntity serverPlayer = serverWorld.getServer()
                .getPlayerManager()
                .getPlayer(responsible);
        PlayerNotification.trySend(serverPlayer, type, Color.RED);
    }
}