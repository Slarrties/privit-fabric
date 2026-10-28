package dev.slarrties.privit.server.region.protection.mixin.interact_with_minecarts;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@AssociatedRule(Rule.INTERACT_WITH_MINECARTS)
@Mixin(VehicleEntity.class)
public abstract class DamageMinecartMixin {

    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void preventMinecartDamage(ServerLevel level, DamageSource source, float amount,
                                       CallbackInfoReturnable<Boolean> cir) {
        if (!((VehicleEntity) (Object) this instanceof AbstractMinecart minecart)) return;
        if (!(source.getEntity() instanceof ServerPlayer serverPlayer)) return;

        boolean allowed = RegionPermissionChecker.isAllowed(serverPlayer.getUUID(), Rule.INTERACT_WITH_MINECARTS, minecart.blockPosition(), serverPlayer.level());
        if (!allowed) {
            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_INTERACT_MINECART, Color.RED);
            cir.setReturnValue(false);
        }
    }
}