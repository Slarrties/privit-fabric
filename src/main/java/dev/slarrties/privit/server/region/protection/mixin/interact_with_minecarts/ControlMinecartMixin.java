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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@AssociatedRule(Rule.INTERACT_WITH_MINECARTS)
@Mixin(AbstractMinecartEntity.class)
public abstract class ControlMinecartMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void forceEjectFromMinecart(CallbackInfo ci) {
        AbstractMinecartEntity minecart = (AbstractMinecartEntity) (Object) this;
        Entity passenger = minecart.getFirstPassenger();
        if (!(passenger instanceof ServerPlayerEntity serverPlayer)) return;

        boolean allowed = PlayerPermissionCache.isAllowed(serverPlayer, Rule.INTERACT_WITH_MINECARTS, minecart.getBlockPos());

        if (!allowed) {
            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_INTERACT_MINECART, Color.RED);
            serverPlayer.stopRiding();
        }
    }
}