package dev.slarrties.privit.server.region.protection.mixin.build.use_item;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.PlaceOnWaterBlockItem;
import net.minecraft.world.item.context.UseOnContext;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@AssociatedRule(Rule.BUILD)
@Mixin(PlaceOnWaterBlockItem.class)
public abstract class PlaceableOnWaterMixin {

    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void preventPlace(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (context.getLevel().isClientSide()) return;
        if (!(context.getPlayer() instanceof ServerPlayer serverPlayer)) return;

        Direction side = context.getClickedFace();
        BlockPos placePos = context.getClickedPos().offset(side.getUnitVec3i());

        if (!RegionPermissionChecker.isAllowed(serverPlayer.getUUID(), Rule.BUILD, placePos, serverPlayer.level())) {
            cir.setReturnValue(InteractionResult.FAIL);
            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_PLACE_BLOCK, Color.RED);
        }
    }
}