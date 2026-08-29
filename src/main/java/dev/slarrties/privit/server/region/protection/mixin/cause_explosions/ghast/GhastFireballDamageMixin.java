package dev.slarrties.privit.server.region.protection.mixin.cause_explosions.ghast;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;

import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@AssociatedRule(Rule.CAUSE_EXPLOSIONS)
@Mixin(FireballEntity.class)
public abstract class GhastFireballDamageMixin {

    @Inject(method = "onEntityHit", at = @At("HEAD"), cancellable = true)
    private void preventGhastFireballDirectDamage(EntityHitResult entityHitResult, CallbackInfo ci) {
        FireballEntity fireball = (FireballEntity) (Object) this;
        Entity target = entityHitResult.getEntity();

        if (fireball.getWorld() instanceof ServerWorld serverWorld) {
            UUID responsible = WorldRegistry.get(serverWorld)
                    .getTrackerManager()
                    .getExplosionOriginTracker()
                    .getResponsiblePlayer(fireball);

            if (responsible == null) return;
            if (!RegionPermissionChecker.isAllowed(responsible, Rule.CAUSE_EXPLOSIONS, target.getBlockPos(), serverWorld)) {
                ServerPlayerEntity serverPlayer = serverWorld.getServer().getPlayerManager().getPlayer(responsible);
                PlayerNotification.trySend(serverPlayer, NotificationType.DENY_CAUSE_EXPLOSION, Color.RED);
                ci.cancel();
            }
        }
    }
}