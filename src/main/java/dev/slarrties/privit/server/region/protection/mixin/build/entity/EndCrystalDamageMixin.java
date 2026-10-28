package dev.slarrties.privit.server.region.protection.mixin.build.entity;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.util.DamageResponsibilityChecker;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;
import dev.slarrties.privit.server.tracking.protection.ExplosionOriginTracker;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@AssociatedRule(Rule.BUILD)
@Mixin(EndCrystal.class)
public abstract class EndCrystalDamageMixin {

    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void preventDamageAndTrackExplosion(ServerLevel serverLevel, DamageSource source, float damage, CallbackInfoReturnable<Boolean> cir) {
        EndCrystal crystal = (EndCrystal) (Object) this;
        UUID responsible = DamageResponsibilityChecker.getResponsibleAttacker(source, serverLevel);

        if (responsible != null) {
            BlockPos pos = crystal.blockPosition();

            if (!RegionPermissionChecker.isAllowed(responsible, Rule.BUILD, pos, serverLevel)) {
                if (source.getEntity() instanceof ServerPlayer serverPlayer) {
                    PlayerNotification.trySend(serverPlayer, NotificationType.DENY_BREAK_BLOCK, Color.RED);
                }
                cir.setReturnValue(false);
                return;
            }
        }

        ExplosionOriginTracker explosionTracker = WorldRegistry.get(serverLevel)
                .getTrackerManager()
                .getExplosionOriginTracker();

        if (responsible != null) {
            explosionTracker.record(crystal, responsible);
        } else {
            Entity attacker = source.getEntity();

            if (attacker instanceof ServerPlayer serverPlayer) {
                explosionTracker.record(crystal, serverPlayer.getUUID());
            } else if (attacker instanceof Projectile projectile) {
                if (projectile.getOwner() instanceof ServerPlayer serverPlayer) {
                    explosionTracker.record(crystal, serverPlayer.getUUID());
                }
            }
        }
    }
}