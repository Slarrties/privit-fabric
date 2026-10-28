package dev.slarrties.privit.server.region.protection.mixin.build.use_item;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.context.UseOnContext;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@AssociatedRule(Rule.BUILD)
@Mixin(HoeItem.class)
public abstract class HoeTillMixin {

    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void preventTill(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (context.getLevel().isClientSide()) return;
        if (!(context.getPlayer() instanceof ServerPlayer serverPlayer)) return;

        BlockPos pos = context.getClickedPos();
        if (!RegionPermissionChecker.isAllowed(serverPlayer.getUUID(), Rule.BUILD, pos, serverPlayer.level())) {
            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_BREAK_BLOCK, Color.RED);
            cir.setReturnValue(InteractionResult.PASS);
        }
    }
}