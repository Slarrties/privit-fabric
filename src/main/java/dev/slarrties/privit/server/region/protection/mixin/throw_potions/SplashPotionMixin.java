package dev.slarrties.privit.server.region.protection.mixin.throw_potions;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;
import dev.slarrties.privit.server.tracking.protection.CampfireOriginTracker;

import net.minecraft.block.BlockState;
import net.minecraft.block.CampfireBlock;
import net.minecraft.item.Items;
import net.minecraft.potion.Potion;
import net.minecraft.potion.Potions;
import net.minecraft.potion.PotionUtil;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@AssociatedRule({Rule.THROW_POTIONS, Rule.EXTINGUISH_FIRE})
@Mixin(PotionEntity.class)
public abstract class SplashPotionMixin {

    @Unique
    private static final int POTION_EFFECT_RADIUS_CHECK = 4;

    @Inject(method = "applySplashPotion", at = @At("HEAD"), cancellable = true)
    private void cancelSplashInProtectedRegion(List<StatusEffectInstance> effects, Entity entity, CallbackInfo ci) {
        PotionEntity potion = (PotionEntity) (Object) this;
        Entity owner = potion.getOwner();
        if (!(owner instanceof ServerPlayerEntity thrower)) return;

        Vec3d impactPos = potion.getPos();
        boolean allowed = isPotionAreaSafe(thrower, impactPos);
        if (!allowed) {
            ci.cancel();
            sendDenyNotification(thrower);
        }
    }

    @Inject(method = "applyLingeringPotion", at = @At("HEAD"), cancellable = true)
    private void cancelLingeringPotionInProtectedRegion(ItemStack stack, Potion potion, CallbackInfo ci) {
        PotionEntity potion2 = (PotionEntity) (Object) this;
        Entity owner = potion2.getOwner();
        if (!(owner instanceof ServerPlayerEntity thrower)) return;

        Vec3d impactPos = potion2.getPos();
        boolean allowed = isPotionAreaSafe(thrower, impactPos);
        if (!allowed) {
            sendDenyNotification(thrower);
            ci.cancel();
        }
    }

    @Inject(method = "onBlockHit", at = @At("HEAD"), cancellable = true)
    private void preventWaterBottleExtinguishingBlocks(BlockHitResult blockHitResult, CallbackInfo ci) {
        PotionEntity potion = (PotionEntity) (Object) this;
        if (potion.getWorld().isClient) return;
        if (!isWaterBottle(potion)) return;

        Entity owner = potion.getOwner();
        if (!(owner instanceof ServerPlayerEntity thrower)) return;

        BlockPos pos = blockHitResult.getBlockPos();
        BlockState state = potion.getWorld().getBlockState(pos);

        if (state.getBlock() instanceof CampfireBlock && state.get(CampfireBlock.LIT)) {
            if (!RegionPermissionChecker.isAllowed(thrower, Rule.EXTINGUISH_FIRE, pos)) {
                sendExtinguishDenyNotification(thrower);
                ci.cancel();
                return;
            }

            if (potion.getWorld() instanceof ServerWorld serverWorld) {
                CampfireOriginTracker tracker = WorldRegistry.get(serverWorld)
                        .getTrackerManager()
                        .getCampfireOriginTracker();
                tracker.remove(pos);
            }
        }
    }

    @Inject(method = "applyWater", at = @At("HEAD"), cancellable = true)
    private void preventWaterBottleExtinguishingEntities(CallbackInfo ci) {
        PotionEntity potion = (PotionEntity) (Object) this;
        if (potion.getWorld().isClient) return;
        if (!isWaterBottle(potion)) return;

        Entity owner = potion.getOwner();
        if (!(owner instanceof ServerPlayerEntity thrower)) return;

        BlockPos pos = potion.getBlockPos();
        if (!RegionPermissionChecker.isAllowed(thrower, Rule.EXTINGUISH_FIRE, pos)) {
            sendExtinguishDenyNotification(thrower);
            ci.cancel();
        }
    }

    @Unique
    private boolean isWaterBottle(PotionEntity potion) {
        var stack = potion.getStack();
        if (!stack.isOf(Items.SPLASH_POTION) && !stack.isOf(Items.LINGERING_POTION)) return false;
        return PotionUtil.getPotion(stack) == Potions.WATER;
    }

    @Unique
    private static boolean isPotionAreaSafe(ServerPlayerEntity thrower, Vec3d center) {
        final int r = POTION_EFFECT_RADIUS_CHECK;
        BlockPos centerPos = BlockPos.ofFloored(center);

        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (Math.abs(dx) + Math.abs(dz) > r + 1) continue;
                BlockPos checkPos = centerPos.add(dx, 0, dz);
                if (!RegionPermissionChecker.isAllowed(thrower, Rule.THROW_POTIONS, checkPos)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Unique
    private void sendDenyNotification(ServerPlayerEntity player) {
        PlayerNotification.trySend(player, NotificationType.DENY_THROW_POTION, Color.RED);
    }

    @Unique
    private void sendExtinguishDenyNotification(ServerPlayerEntity player) {
        PlayerNotification.trySend(player, NotificationType.DENY_EXTINGUISH_FIRE, Color.RED);
    }
}