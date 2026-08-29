package dev.slarrties.privit.server.region.protection.mixin.throw_wind_charges;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;

import net.minecraft.entity.Entity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.entity.projectile.AbstractWindChargeEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@AssociatedRule(Rule.THROW_WIND_CHARGES)
@Mixin(AbstractWindChargeEntity.class)
public abstract class WindChargeEntityMixin {

    @Inject(method = "onCollision", at = @At("HEAD"), cancellable = true)
    private void cancelWindChargeInProtectedRegion(HitResult hitResult, CallbackInfo ci) {
        AbstractWindChargeEntity charge = (AbstractWindChargeEntity) (Object) this;
        if (charge.getWorld().isClient) return;

        Entity owner = charge.getOwner();
        if (!(owner instanceof ServerPlayerEntity serverPlayer)) return;

        BlockPos impactPos = BlockPos.ofFloored(hitResult.getPos());
        boolean allowed = RegionPermissionChecker.isAllowed(serverPlayer, Rule.THROW_WIND_CHARGES, impactPos);

        if (!allowed) {
            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_THROW_WIND_CHARGE, Color.RED);
            charge.discard();
            ci.cancel();
        }
    }
}