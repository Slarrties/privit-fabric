package dev.slarrties.privit.server.region.protection.mixin.throw_ender_pearls;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@AssociatedRule(Rule.THROW_ENDER_PEARLS)
@Mixin(ThrownEnderpearl.class)
public abstract class ThrowEnderPearlMixin {

    @Inject(method = "onHit", at = @At("HEAD"), cancellable = true)
    private void cancelTeleportInProtectedRegion(HitResult hitResult, CallbackInfo ci) {
        ThrownEnderpearl pearl = (ThrownEnderpearl) (Object) this;
        if (pearl.level().isClientSide()) return;

        Entity owner = pearl.getOwner();
        if (!(owner instanceof ServerPlayer serverPlayer)) return;

        Vec3 landingPos = hitResult.getLocation();
        BlockPos landingBlockPos = BlockPos.containing(landingPos);

        boolean allowed = RegionPermissionChecker.isAllowed(serverPlayer.getUUID(), Rule.THROW_ENDER_PEARLS, landingBlockPos, serverPlayer.level());
        if (!allowed) {
            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_THROW_ENDER_PEARL, Color.RED);
            pearl.discard();
            ci.cancel();
        }
    }
}