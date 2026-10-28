package dev.slarrties.privit.server.region.protection.mixin.throw_potions;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;
import dev.slarrties.privit.server.tracking.protection.CampfireOriginTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.AbstractThrownPotion;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownSplashPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;

// TODO
@AssociatedRule({Rule.THROW_POTIONS, Rule.EXTINGUISH_FIRE})
@Mixin(ThrownSplashPotion.class)
public abstract class SplashPotionMixin {

    @Unique
    private static final int POTION_EFFECT_RADIUS_CHECK = 4;

    // Метод applyEffects находится именно в ThrownSplashPotion
//    @Inject(method = "applyEffects", at = @At("HEAD"), cancellable = true)
//    private void cancelSplashInProtectedRegion(Entity entity, Collection<MobEffectInstance> effects, CallbackInfo ci) {
//        ThrownSplashPotion potion = (ThrownSplashPotion) (Object) this;
//        Entity owner = potion.getOwner();
//        if (!(owner instanceof ServerPlayer thrower)) return;
//        if (!(potion.level() instanceof ServerLevel serverLevel)) return;
//
//        Vec3 impactPos = potion.position();
//        boolean allowed = isPotionAreaSafe(thrower, impactPos, serverLevel);
//        if (!allowed) {
//            ci.cancel();
//            PlayerNotification.trySend(thrower, NotificationType.DENY_THROW_POTION, Color.RED);
//        }
//    }
//
//    // Метод onHitBlock находится в AbstractThrownPotion (родитель),
//    // но Mixin позволяет инжектиться в него через дочерний класс
//    @Inject(method = "onHitBlock", at = @At("HEAD"), cancellable = true)
//    private void preventWaterBottleExtinguishingBlocks(BlockHitResult blockHitResult, CallbackInfo ci) {
//        AbstractThrownPotion potion = (AbstractThrownPotion) (Object) this;
//        if (potion.level().isClientSide()) return;
//        if (!(potion.level() instanceof ServerLevel serverLevel)) return;
//        if (!isWaterBottle(potion)) return;
//
//        Entity owner = potion.getOwner();
//        if (!(owner instanceof ServerPlayer thrower)) return;
//
//        BlockPos pos = blockHitResult.getBlockPos();
//        BlockState state = potion.level().getBlockState(pos);
//
//        if (state.getBlock() instanceof CampfireBlock && state.getValue(CampfireBlock.LIT)) {
//            if (!RegionPermissionChecker.isAllowed(thrower.getUUID(), Rule.EXTINGUISH_FIRE, pos, serverLevel)) {
//                PlayerNotification.trySend(thrower, NotificationType.DENY_EXTINGUISH_FIRE, Color.RED);
//                ci.cancel();
//                return;
//            }
//            CampfireOriginTracker tracker = WorldRegistry.get(serverLevel)
//                    .getTrackerManager()
//                    .getCampfireOriginTracker();
//            tracker.remove(pos);
//        }
//    }
//
//    @Unique
//    private boolean isWaterBottle(AbstractThrownPotion potion) {
//        ItemStack stack = potion.getItem();
//        if (!stack.is(Items.SPLASH_POTION)) return false;
//
//        PotionContentsComponent contents = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContentsComponent.EMPTY);
//        return contents.potion().map(holder -> holder.is(Potions.WATER)).orElse(false);
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