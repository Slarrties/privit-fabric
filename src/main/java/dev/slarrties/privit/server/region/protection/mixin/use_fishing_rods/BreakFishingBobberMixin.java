package dev.slarrties.privit.server.region.protection.mixin.use_fishing_rods;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;
import dev.slarrties.privit.server.util.PlayerNotification;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@AssociatedRule(Rule.USE_FISHING_RODS)
@Mixin(FishingBobberEntity.class)
public abstract class BreakFishingBobberMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void breakBobberInProtectedRegion(CallbackInfo ci) {
        FishingBobberEntity bobber = (FishingBobberEntity) (Object) this;
        if (bobber.getWorld().isClient) return;

        Entity owner = bobber.getOwner();
        if (!(owner instanceof ServerPlayerEntity serverPlayer)) return;

        BlockPos bobberPos = bobber.getBlockPos();
        boolean allowed = RegionPermissionChecker.isAllowed(serverPlayer, Rule.USE_FISHING_RODS, bobberPos);

        if (!allowed) {
            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_USE_FISHING_ROD, Color.RED);
            bobber.discard();
        }
    }
}