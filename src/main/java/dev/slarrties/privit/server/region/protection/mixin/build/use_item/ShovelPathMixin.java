package dev.slarrties.privit.server.region.protection.mixin.build.use_item;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;

import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.BlockState;
import net.minecraft.block.CampfireBlock;
import net.minecraft.item.ShovelItem;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.network.ServerPlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@AssociatedRule(Rule.BUILD)
@Mixin(ShovelItem.class)
public abstract class ShovelPathMixin {

    @Inject(method = "useOnBlock", at = @At("HEAD"), cancellable = true)
    private void preventPath(ItemUsageContext context, CallbackInfoReturnable<ActionResult> cir) {
        if (context.getWorld().isClient()) return;
        if (!(context.getPlayer() instanceof ServerPlayerEntity serverPlayer)) return;

        BlockPos pos = context.getBlockPos();
        BlockState state = context.getWorld().getBlockState(pos);

        if (state.getBlock() instanceof CampfireBlock) return;
        if (!RegionPermissionChecker.isAllowed(serverPlayer.getUuid(), Rule.BUILD, pos, serverPlayer.getServerWorld())) {
            cir.setReturnValue(ActionResult.FAIL);
            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_BREAK_BLOCK, Color.RED);
        }
    }
}