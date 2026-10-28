package dev.slarrties.privit.server.region.protection.mixin.use_fire_starters;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.tracking.protection.FireOriginTracker;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;
import dev.slarrties.privit.server.tracking.redstone.handler.RedstoneReceiverHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@AssociatedRule(Rule.USE_FIRE_STARTERS)
@Mixin(SmallFireball.class)
public abstract class FireballCollisionMixin {

    @Inject(method = "onHit(Lnet/minecraft/world/phys/HitResult;)V", at = @At("HEAD"), cancellable = true)
    private void cancelFireChargeInProtectedRegion(HitResult hitResult, CallbackInfo ci) {
        SmallFireball fireball = (SmallFireball) (Object) this;
        if (fireball.level().isClientSide()) return;

        Entity owner = fireball.getOwner();
        UUID responsible;
        BlockPos impactPos = BlockPos.containing(hitResult.getLocation());

        if (fireball.level() instanceof ServerLevel serverLevel) {
            if (owner instanceof ServerPlayer serverPlayer) {
                responsible = serverPlayer.getUUID();
            } else {
                responsible = RedstoneReceiverHandler.findResponsiblePlayer(serverLevel, impactPos);
            }

            if (responsible == null) return;

            boolean allowed = RegionPermissionChecker.isAllowed(responsible, Rule.USE_FIRE_STARTERS, impactPos, serverLevel);
            if (!allowed) {
                ServerPlayer serverPlayer = serverLevel.getServer().getPlayerList().getPlayer(responsible);
                PlayerNotification.trySend(serverPlayer, NotificationType.DENY_USE_FIRE_STARTER, Color.RED);
                fireball.discard();
                ci.cancel();
            } else {
                FireOriginTracker fireTracker = WorldRegistry.get(serverLevel)
                        .getTrackerManager()
                        .getFireOriginTracker();
                fireTracker.record(this.calculateFirePlacementPos(hitResult), responsible);
            }
        }
    }

    @Unique
    private BlockPos calculateFirePlacementPos(HitResult hitResult) {
        if (!(hitResult instanceof BlockHitResult blockHit)) return BlockPos.containing(hitResult.getLocation());
        BlockPos hitBlock = blockHit.getBlockPos();
        Direction side = blockHit.getDirection();

        return hitBlock.relative(side);
    }
}