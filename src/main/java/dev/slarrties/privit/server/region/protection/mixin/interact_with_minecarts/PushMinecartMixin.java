package dev.slarrties.privit.server.region.protection.mixin.interact_with_minecarts;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.PlayerPermissionCache;

import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.server.network.ServerPlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@AssociatedRule(Rule.INTERACT_WITH_MINECARTS)
@Mixin(AbstractMinecartEntity.class)
public abstract class PushMinecartMixin {

    @Inject(method = "isPushable", at = @At("HEAD"), cancellable = true)
    private void preventPlayerPushingMinecart(CallbackInfoReturnable<Boolean> cir) {
        AbstractMinecartEntity cart = (AbstractMinecartEntity) (Object) this;
        if (cart.getWorld().isClient) return;

        if (cart.hasPassengers()) {
            Entity passenger = cart.getFirstPassenger();

            if (passenger instanceof ServerPlayerEntity serverPlayer) {
                boolean allowed = PlayerPermissionCache.isAllowed(serverPlayer, Rule.INTERACT_WITH_MINECARTS, cart.getBlockPos());

                if (!allowed) {
                    PlayerNotification.trySend(serverPlayer, NotificationType.DENY_INTERACT_MINECART, Color.RED);
                    cir.setReturnValue(false);
                    return;
                }
            }
        }

        ServerPlayerEntity closestPlayer = (ServerPlayerEntity) cart.getWorld().getClosestPlayer(cart, 1.0);

        if (closestPlayer != null) {
            boolean allowed = PlayerPermissionCache.isAllowed(closestPlayer, Rule.INTERACT_WITH_MINECARTS, cart.getBlockPos());

            if (!allowed) {
                PlayerNotification.trySend(closestPlayer, NotificationType.DENY_INTERACT_MINECART, Color.RED);
                cir.setReturnValue(false);
            }
        }
    }
}