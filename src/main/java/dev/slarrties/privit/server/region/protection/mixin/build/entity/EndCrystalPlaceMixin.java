package dev.slarrties.privit.server.region.protection.mixin.build.entity;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;

import net.minecraft.item.EndCrystalItem;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.server.network.ServerPlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@AssociatedRule(Rule.BUILD)
@Mixin(EndCrystalItem.class)
public abstract class EndCrystalPlaceMixin {

    @Inject(method = "useOnBlock", at = @At("HEAD"), cancellable = true)
    private void preventPlace(ItemUsageContext context, CallbackInfoReturnable<ActionResult> cir) {
        if (context.getWorld().isClient()) return;
        if (!(context.getPlayer() instanceof ServerPlayerEntity serverPlayer)) return;

        Direction side = context.getSide();
        BlockPos placePos = context.getBlockPos().offset(side);

        if (!RegionPermissionChecker.isAllowed(serverPlayer.getUuid(), Rule.BUILD, placePos, serverPlayer.getServerWorld())) {
            cir.setReturnValue(ActionResult.FAIL);
            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_PLACE_BLOCK, Color.RED);
        }
    }
}