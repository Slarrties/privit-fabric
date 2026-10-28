package dev.slarrties.privit.server.region.protection.mixin.cause_explosions.wither;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.projectile.hurtingprojectile.WitherSkull;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@AssociatedRule(Rule.CAUSE_EXPLOSIONS)
@Mixin(WitherSkull.class)
public abstract class WitherSkullDamageMixin {

    @Inject(method = "onHitEntity", at = @At("HEAD"), cancellable = true)
    private void preventWitherSkullDamage(EntityHitResult entityHitResult, CallbackInfo ci) {
        WitherSkull skull = (WitherSkull) (Object) this;
        Entity target = entityHitResult.getEntity();
        Entity owner = skull.getOwner();

        if (owner != null && owner.level() instanceof ServerLevel serverLevel) {
            UUID responsible = null;
            if (owner instanceof WitherBoss wither) {
                responsible = WorldRegistry.get(serverLevel)
                        .getTrackerManager()
                        .getExplosionOriginTracker()
                        .getResponsible(wither);
            }
            if (responsible == null) return;
            if (!RegionPermissionChecker.isAllowed(responsible, Rule.CAUSE_EXPLOSIONS, target.blockPosition(), serverLevel)) {
                ServerPlayer serverPlayer = serverLevel.getServer()
                        .getPlayerList()
                        .getPlayer(responsible);
                PlayerNotification.trySend(serverPlayer, NotificationType.DENY_CAUSE_EXPLOSION, Color.RED);
                ci.cancel();
            }
        }
    }
}