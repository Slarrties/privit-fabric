package dev.slarrties.privit.server.region.protection.mixin.throw_potions;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownLingeringPotion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// TODO
@AssociatedRule(Rule.THROW_POTIONS)
@Mixin(ThrownLingeringPotion.class)
public abstract class LingeringPotionMixin {

    @Unique
    private static final int POTION_EFFECT_RADIUS_CHECK = 4;

//    // Метод makeAreaOfEffectCloud находится именно в ThrownLingeringPotion
//    @Inject(method = "makeAreaOfEffectCloud", at = @At("HEAD"), cancellable = true)
//    private void cancelLingeringPotionInProtectedRegion(PotionContents potionContents, CallbackInfo ci) {
//        ThrownLingeringPotion potion = (ThrownLingeringPotion) (Object) this;
//        Entity owner = potion.getOwner();
//        if (!(owner instanceof ServerPlayer thrower)) return;
//        if (!(potion.level() instanceof ServerLevel serverLevel)) return;
//
//        Vec3 impactPos = potion.position();
//        boolean allowed = isPotionAreaSafe(thrower, impactPos, serverLevel);
//        if (!allowed) {
//            PlayerNotification.trySend(thrower, NotificationType.DENY_THROW_POTION, Color.RED);
//            ci.cancel();
//        }
//    }
//
//    @Unique
//    private static boolean isPotionAreaSafe(ServerPlayer thrower, Vec3 center, ServerLevel serverLevel) {
//        final int r = POTION_EFFECT_RADIUS_CHECK;
//        for (int dx = -r; dx <= r; dx++) {
//            for (int dz = -r; dz <= r; dz++) {
//                if (Math.abs(dx) + Math.abs(dz) > r + 1) continue;
//                BlockPos checkPos = BlockPos.containing(center.x + dx, center.y, center.z + dz);
//                if (!RegionPermissionChecker.isAllowed(thrower.getUUID(), Rule.THROW_POTIONS, checkPos, serverLevel)) {
//                    return false;
//                }
//            }
//        }
//        return true;
//    }
}