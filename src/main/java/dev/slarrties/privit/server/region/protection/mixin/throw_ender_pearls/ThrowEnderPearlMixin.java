package dev.slarrties.privit.server.region.protection.mixin.throw_ender_pearls;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;

import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.server.network.ServerPlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@AssociatedRule(Rule.THROW_ENDER_PEARLS)
@Mixin(EnderPearlEntity.class)
public abstract class ThrowEnderPearlMixin {

    @Inject(method = "onCollision", at = @At("HEAD"), cancellable = true)
    private void cancelTeleportInProtectedRegion(HitResult hitResult, CallbackInfo ci) {
        EnderPearlEntity pearl = (EnderPearlEntity) (Object) this;
        if (pearl.getWorld().isClient) return;

        Entity owner = pearl.getOwner();
        if (!(owner instanceof ServerPlayerEntity serverPlayer)) return;

        Vec3d landingPos = hitResult.getPos();
        BlockPos landingBlockPos = BlockPos.ofFloored(landingPos);
        boolean allowed = RegionPermissionChecker.isAllowed(serverPlayer, Rule.THROW_ENDER_PEARLS, landingBlockPos);

        if (!allowed) {
            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_THROW_ENDER_PEARL, Color.RED);
            pearl.discard();
            ci.cancel();
        }
    }
}