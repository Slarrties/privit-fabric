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

import net.minecraft.util.math.BlockPos;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@AssociatedRule(Rule.BUILD)
@Mixin(EndCrystalEntity.class)
public abstract class EndCrystalDamageMixin {

    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void preventDamageAndTrackExplosion(DamageSource source, float amount,
                                                CallbackInfoReturnable<Boolean> cir) {
        EndCrystalEntity crystal = (EndCrystalEntity) (Object) this;
        if (!(crystal.getWorld() instanceof ServerWorld serverWorld)) return;

        UUID responsible = DamageResponsibilityChecker.getResponsibleAttacker(source, serverWorld);

        if (responsible != null) {
            BlockPos pos = crystal.getBlockPos();

            if (!RegionPermissionChecker.isAllowed(responsible, Rule.BUILD, pos, serverWorld)) {
                cir.setReturnValue(false);

                if (source.getAttacker() instanceof ServerPlayerEntity serverPlayer)
                    PlayerNotification.trySend(serverPlayer, NotificationType.DENY_BREAK_BLOCK, Color.RED);

                return;
            }
        }

        ExplosionOriginTracker explosionTracker = WorldRegistry.get(serverWorld)
                .getTrackerManager()
                .getExplosionOriginTracker();

        if (responsible != null) {
            explosionTracker.record(crystal, responsible);
        } else {
            Entity attacker = source.getAttacker();

            if (attacker instanceof ServerPlayerEntity serverPlayer) {
                explosionTracker.record(crystal, serverPlayer.getUuid());
            } else if (attacker instanceof ProjectileEntity projectile) {
                Entity owner = projectile.getOwner();

                if (owner instanceof ServerPlayerEntity serverPlayer) {
                    explosionTracker.record(crystal, serverPlayer.getUuid());
                }
            }
        }
    }
}