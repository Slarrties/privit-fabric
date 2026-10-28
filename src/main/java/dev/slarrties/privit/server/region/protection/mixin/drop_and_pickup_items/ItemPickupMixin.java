package dev.slarrties.privit.server.region.protection.mixin.drop_and_pickup_items;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.PlayerPermissionCache;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@AssociatedRule(Rule.DROP_AND_PICKUP_ITEMS)
@Mixin(ItemEntity.class)
public abstract class ItemPickupMixin {

    @Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
    private void preventItemPickup(Player player, CallbackInfo ci) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        ItemEntity itemEntity = (ItemEntity) (Object) this;
        boolean allowed = PlayerPermissionCache.isAllowed(serverPlayer, Rule.DROP_AND_PICKUP_ITEMS, itemEntity.blockPosition());
        if (!allowed) {
            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_ITEM_PICKUP, Color.RED);
            ci.cancel();
        }
    }
}