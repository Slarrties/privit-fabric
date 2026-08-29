package dev.slarrties.privit.server.region.protection.mixin.drop_and_pickup_items;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.PlayerPermissionCache;

import net.minecraft.item.ItemStack;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@AssociatedRule(Rule.DROP_AND_PICKUP_ITEMS)
@Mixin(PlayerEntity.class)
public abstract class InventoryDropMixin {

    @Inject(
            method = "dropItem(Lnet/minecraft/item/ItemStack;ZZ)Lnet/minecraft/entity/ItemEntity;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void preventDropFromInventory(ItemStack stack, boolean throwRandomly, boolean retainOwnership,
                                          CallbackInfoReturnable<ItemEntity> cir) {
        if (!((PlayerEntity) (Object) this instanceof ServerPlayerEntity serverPlayer)) return;
        if (throwRandomly) return; // Q / Ctrl+Q

        boolean allowed = PlayerPermissionCache.isAllowed(serverPlayer, Rule.DROP_AND_PICKUP_ITEMS, serverPlayer.getBlockPos());
        if (allowed) return;

        if (!stack.isEmpty()) {
            if (serverPlayer.currentScreenHandler.getCursorStack().isEmpty()) {
                serverPlayer.currentScreenHandler.setCursorStack(stack);
            } else {
                boolean inserted = serverPlayer.getInventory().insertStack(stack);
                if (!inserted) serverPlayer.currentScreenHandler.setCursorStack(stack);
            }
            serverPlayer.currentScreenHandler.sendContentUpdates();
        }

        PlayerNotification.trySend(serverPlayer, NotificationType.DENY_ITEM_PICKUP, Color.RED);
        cir.setReturnValue(null);
    }
}