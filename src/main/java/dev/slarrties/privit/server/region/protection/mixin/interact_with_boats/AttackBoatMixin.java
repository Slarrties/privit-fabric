package dev.slarrties.privit.server.region.protection.mixin.interact_with_boats;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;

import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.entity.vehicle.VehicleEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@AssociatedRule(Rule.INTERACT_WITH_BOATS)
@Mixin(VehicleEntity.class)
public abstract class AttackBoatMixin {

    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void preventVehicleDamageByPlayer(DamageSource source, float amount,
                                              CallbackInfoReturnable<Boolean> cir) {
        if (!((VehicleEntity) (Object) this instanceof BoatEntity boat)) return;
        if (!(source.getAttacker() instanceof ServerPlayerEntity serverPlayer)) return;

        boolean allowed = RegionPermissionChecker.isAllowed(serverPlayer, Rule.INTERACT_WITH_BOATS, boat.getBlockPos());
        if (!allowed) {
            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_INTERACT_BOAT, Color.RED);
            cir.setReturnValue(false);
        }
    }
}