package dev.slarrties.privit.server.region.protection.mixin.throw_eggs;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEgg;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@AssociatedRule(Rule.THROW_EGGS)
@Mixin(ThrownEgg.class)
public abstract class EggCollisionMixin {

    @Inject(method = "onHit", at = @At("HEAD"), cancellable = true)
    private void cancelEggInProtectedRegion(HitResult hitResult, CallbackInfo ci) {
        ThrownEgg egg = (ThrownEgg) (Object) this;
        if (egg.level().isClientSide()) return;

        Entity owner = egg.getOwner();
        if (!(owner instanceof ServerPlayer serverPlayer)) return;

        BlockPos impactPos = BlockPos.containing(hitResult.getLocation());
        boolean allowed = RegionPermissionChecker.isAllowed(serverPlayer.getUUID(), Rule.THROW_EGGS, impactPos, serverPlayer.level());
        if (!allowed) {
            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_THROW_EGG, Color.RED);
            egg.discard();
            ci.cancel();
        }
    }
}