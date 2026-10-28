package dev.slarrties.privit.server.region.protection.mixin.use_fishing_rods;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;

import net.minecraft.world.entity.projectile.FishingHook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@AssociatedRule(Rule.USE_FISHING_RODS)
@Mixin(FishingHook.class)
public abstract class BreakFishingBobberMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void breakBobberInProtectedRegion(CallbackInfo ci) {
        FishingHook bobber = (FishingHook) (Object) this;
        if (bobber.level().isClientSide()) return;

        Entity owner = bobber.getOwner();
        if (!(owner instanceof ServerPlayer serverPlayer)) return;

        BlockPos bobberPos = bobber.blockPosition();
        boolean allowed = RegionPermissionChecker.isAllowed(serverPlayer.getUUID(), Rule.USE_FISHING_RODS, bobberPos, serverPlayer.level());
        if (!allowed) {
            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_USE_FISHING_ROD, Color.RED);
            bobber.discard();
        }
    }
}