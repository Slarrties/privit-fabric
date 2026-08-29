package dev.slarrties.privit.server.region.protection.mixin.drop_and_pickup_items;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.PlayerPermissionCache;

import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@AssociatedRule(Rule.DROP_AND_PICKUP_ITEMS)
@Mixin(ItemEntity.class)
public abstract class ItemPickupMixin {

    @Inject(method = "onPlayerCollision", at = @At("HEAD"), cancellable = true)
    private void preventItemPickup(PlayerEntity player, CallbackInfo ci) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;

        ItemEntity itemEntity = (ItemEntity) (Object) this;
        boolean allowed = PlayerPermissionCache.isAllowed(serverPlayer, Rule.DROP_AND_PICKUP_ITEMS, itemEntity.getBlockPos());

        if (!allowed) {
            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_ITEM_PICKUP, Color.RED);
            ci.cancel();
        }
    }
}