package dev.slarrties.privit.server.region.protection.mixin.use_leashes;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.LeadItem;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@AssociatedRule(Rule.USE_LEASHES)
@Mixin(LeadItem.class)
public abstract class UseLeashOnBlockMixin {

    @Inject(method = "bindPlayerMobs", at = @At("HEAD"), cancellable = true)
    private static void preventAttachToBlock(Player player, Level level, BlockPos pos, CallbackInfoReturnable<InteractionResult> cir) {
        if (level.isClientSide()) return;
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        if (!RegionPermissionChecker.isAllowed(serverPlayer.getUUID(), Rule.USE_LEASHES, pos, serverPlayer.level())) {
            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_USE_LEASH, Color.RED);
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}