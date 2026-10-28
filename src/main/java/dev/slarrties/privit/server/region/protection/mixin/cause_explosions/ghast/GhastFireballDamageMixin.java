package dev.slarrties.privit.server.region.protection.mixin.cause_explosions.ghast;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;

import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.entity.projectile.hurtingprojectile.Fireball;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@AssociatedRule(Rule.CAUSE_EXPLOSIONS)
@Mixin(Projectile.class)
public abstract class GhastFireballDamageMixin {

    @Inject(method = "hitTargetOrDeflectSelf", at = @At("HEAD"), cancellable = true)
    private void preventGhastFireballDirectDamage(HitResult hitResult, CallbackInfoReturnable<ProjectileDeflection> cir) {
        if (!(hitResult instanceof EntityHitResult entityHitResult)) return;
        if (!(((Object) this) instanceof Fireball fireball)) return; // TODO: is it works?

        Entity target = entityHitResult.getEntity();
        if (fireball.level() instanceof ServerLevel serverLevel) {
            UUID responsible = WorldRegistry.get(serverLevel)
                    .getTrackerManager()
                    .getExplosionOriginTracker()
                    .getResponsible(fireball);

            if (responsible == null) return;

            if (!RegionPermissionChecker.isAllowed(responsible, Rule.CAUSE_EXPLOSIONS, target.blockPosition(), serverLevel)) {
                ServerPlayer serverPlayer = serverLevel.getServer()
                        .getPlayerList()
                        .getPlayer(responsible);
                PlayerNotification.trySend(serverPlayer, NotificationType.DENY_CAUSE_EXPLOSION, Color.RED);
                cir.cancel();
            }
        }
    }
}