package dev.slarrties.privit.server.region.protection.mixin.use_leashes;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;

import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.decoration.LeashKnotEntity;
import net.minecraft.server.network.ServerPlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@AssociatedRule(Rule.USE_LEASHES)
@Mixin(LeashKnotEntity.class)
public abstract class UseLeashMixin {

    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void preventInteract(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if (player.getWorld().isClient()) return;
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;

        LeashKnotEntity self = (LeashKnotEntity) (Object) this;
        BlockPos pos = self.getBlockPos();

        if (!RegionPermissionChecker.isAllowed(serverPlayer, Rule.USE_LEASHES, pos)) {
            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_USE_LEASH, Color.RED);
            cir.setReturnValue(ActionResult.FAIL);
        }
    }
}