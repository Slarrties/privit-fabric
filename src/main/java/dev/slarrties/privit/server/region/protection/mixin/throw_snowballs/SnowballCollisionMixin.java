package dev.slarrties.privit.server.region.protection.mixin.throw_snowballs;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;

import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@AssociatedRule(Rule.THROW_SNOWBALLS)
@Mixin(SnowballEntity.class)
public abstract class SnowballCollisionMixin {

    @Inject(method = "onCollision(Lnet/minecraft/util/hit/HitResult;)V", at = @At("HEAD"), cancellable = true)
    private void cancelSnowballInProtectedRegion(HitResult hitResult, CallbackInfo ci) {
        SnowballEntity snowball = (SnowballEntity) (Object) this;

        if (snowball.getWorld().isClient) return;
        if (!(snowball.getOwner() instanceof ServerPlayerEntity serverPlayer)) return;

        BlockPos impactPos = BlockPos.ofFloored(hitResult.getPos());
        boolean allowed = RegionPermissionChecker.isAllowed(serverPlayer, Rule.THROW_SNOWBALLS, impactPos);

        if (!allowed) {
            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_THROW_SNOWBALL, Color.RED);
            snowball.discard();
            ci.cancel();
        }
    }
}