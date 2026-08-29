package dev.slarrties.privit.server.region.protection.mixin.interact_with_minecarts;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;

import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.vehicle.VehicleEntity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.server.network.ServerPlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@AssociatedRule(Rule.INTERACT_WITH_MINECARTS)
@Mixin(VehicleEntity.class)
public abstract class DamageMinecartMixin {

    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void preventMinecartDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!(source.getAttacker() instanceof ServerPlayerEntity serverPlayer)) return;

        VehicleEntity vehicle = (VehicleEntity) (Object) this;
        if (!(vehicle instanceof AbstractMinecartEntity)) return;

        boolean allowed = RegionPermissionChecker.isAllowed(serverPlayer, Rule.INTERACT_WITH_MINECARTS, vehicle.getBlockPos());

        if (!allowed) {
            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_INTERACT_MINECART, Color.RED);
            cir.setReturnValue(false);
        }
    }
}