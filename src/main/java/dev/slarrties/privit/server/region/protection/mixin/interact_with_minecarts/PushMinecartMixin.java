package dev.slarrties.privit.server.region.protection.mixin.interact_with_minecarts;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.PlayerPermissionCache;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@AssociatedRule(Rule.INTERACT_WITH_MINECARTS)
@Mixin(AbstractMinecart.class)
public abstract class PushMinecartMixin {

    @Inject(method = "isPushable", at = @At("HEAD"), cancellable = true)
    private void preventPlayerPushingMinecart(CallbackInfoReturnable<Boolean> cir) {
        AbstractMinecart cart = (AbstractMinecart) (Object) this;
        if (cart.level().isClientSide()) return;
        if (!cart.getPassengers().isEmpty()) {
            Entity passenger = cart.getFirstPassenger();
            if (passenger instanceof ServerPlayer serverPlayer) {
                boolean allowed = PlayerPermissionCache.isAllowed(serverPlayer, Rule.INTERACT_WITH_MINECARTS, cart.blockPosition());
                if (!allowed) {
                    PlayerNotification.trySend(serverPlayer, NotificationType.DENY_INTERACT_MINECART, Color.RED);
                    cir.setReturnValue(false);
                    return;
                }
            }
        }

        ServerPlayer closestPlayer = (ServerPlayer) cart.level().getNearestPlayer(cart, 1.0);
        if (closestPlayer != null) {
            boolean allowed = PlayerPermissionCache.isAllowed(closestPlayer, Rule.INTERACT_WITH_MINECARTS, cart.blockPosition());
            if (!allowed) {
                PlayerNotification.trySend(closestPlayer, NotificationType.DENY_INTERACT_MINECART, Color.RED);
                cir.setReturnValue(false);
            }
        }
    }
}