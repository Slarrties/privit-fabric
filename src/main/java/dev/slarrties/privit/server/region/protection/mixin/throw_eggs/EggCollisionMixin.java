package dev.slarrties.privit.server.region.protection.mixin.throw_eggs;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;

import net.minecraft.entity.Entity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.entity.projectile.thrown.EggEntity;
import net.minecraft.server.network.ServerPlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@AssociatedRule(Rule.THROW_EGGS)
@Mixin(EggEntity.class)
public abstract class EggCollisionMixin {

    @Inject(method = "onCollision(Lnet/minecraft/util/hit/HitResult;)V", at = @At("HEAD"), cancellable = true)
    private void cancelEggInProtectedRegion(HitResult hitResult, CallbackInfo ci) {
        EggEntity egg = (EggEntity) (Object) this;
        if (egg.getWorld().isClient) return;

        Entity owner = egg.getOwner();
        if (!(owner instanceof ServerPlayerEntity serverPlayer)) return;

        BlockPos impactPos = BlockPos.ofFloored(hitResult.getPos());
        boolean allowed = RegionPermissionChecker.isAllowed(serverPlayer, Rule.THROW_EGGS, impactPos);

        if (!allowed) {
            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_THROW_EGG, Color.RED);
            egg.discard();
            ci.cancel();
        }
    }
}