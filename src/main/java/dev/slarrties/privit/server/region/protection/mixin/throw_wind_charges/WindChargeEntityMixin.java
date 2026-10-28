package dev.slarrties.privit.server.region.protection.mixin.throw_wind_charges;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.AbstractWindCharge;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@AssociatedRule(Rule.THROW_WIND_CHARGES)
@Mixin(AbstractWindCharge.class)
public abstract class WindChargeEntityMixin {

    @Inject(method = "onHit", at = @At("HEAD"), cancellable = true)
    private void cancelWindChargeInProtectedRegion(HitResult hitResult, CallbackInfo ci) {
        AbstractWindCharge charge = (AbstractWindCharge) (Object) this;
        if (charge.level().isClientSide()) return;

        Entity owner = charge.getOwner();
        if (!(owner instanceof ServerPlayer serverPlayer)) return;

        BlockPos impactPos = BlockPos.containing(hitResult.getLocation());
        boolean allowed = RegionPermissionChecker.isAllowed(serverPlayer.getUUID(), Rule.THROW_WIND_CHARGES, impactPos, serverPlayer.level());
        if (!allowed) {
            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_THROW_WIND_CHARGE, Color.RED);
            charge.discard();
            ci.cancel();
        }
    }
}