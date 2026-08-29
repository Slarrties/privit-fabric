package dev.slarrties.privit.server.region.protection.mixin.drop_and_pickup_items;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.util.InventorySyncSystem;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.PlayerPermissionCache;

import net.minecraft.server.network.ServerPlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@AssociatedRule(Rule.DROP_AND_PICKUP_ITEMS)
@Mixin(ServerPlayerEntity.class)
public abstract class ItemDropMixin {

    @Inject(method = "dropSelectedItem", at = @At("HEAD"), cancellable = true)
    private void preventDropSelectedItem(boolean entireStack, CallbackInfoReturnable<Boolean> cir) {
        ServerPlayerEntity serverPlayer = (ServerPlayerEntity) (Object) this;

        boolean allowed = PlayerPermissionCache.isAllowed(serverPlayer, Rule.DROP_AND_PICKUP_ITEMS, serverPlayer.getBlockPos());
        if (allowed) return;

        InventorySyncSystem.syncFullInventory(serverPlayer);
        PlayerNotification.trySend(serverPlayer, NotificationType.DENY_ITEM_PICKUP, Color.RED);
        cir.setReturnValue(false);
    }
}