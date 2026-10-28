package dev.slarrties.privit.server.region.protection.mixin.drop_and_pickup_items;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.PlayerPermissionCache;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.server.level.ServerPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@AssociatedRule(Rule.DROP_AND_PICKUP_ITEMS)
@Mixin(Player.class)
public abstract class InventoryDropMixin {

    @Inject(
            method = "drop(Lnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/world/entity/item/ItemEntity;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void preventDropFromInventory(ItemStack itemStack, boolean thrownFromHand, CallbackInfoReturnable<ItemEntity> cir) {
        if (!((Player) (Object) this instanceof ServerPlayer serverPlayer)) return;
        if (thrownFromHand) return;

        boolean allowed = PlayerPermissionCache.isAllowed(serverPlayer, Rule.DROP_AND_PICKUP_ITEMS, serverPlayer.blockPosition());
        if (allowed) return;
        if (!itemStack.isEmpty()) {
            if (serverPlayer.containerMenu.getCarried().isEmpty()) {
                serverPlayer.containerMenu.setCarried(itemStack);
            } else {
                boolean inserted = serverPlayer.getInventory().add(itemStack);
                if (!inserted) {
                    serverPlayer.containerMenu.setCarried(itemStack);
                }
            }
            serverPlayer.containerMenu.broadcastChanges();
        }

        PlayerNotification.trySend(serverPlayer, NotificationType.DENY_ITEM_PICKUP, Color.RED);
        cir.setReturnValue(null);
    }
}