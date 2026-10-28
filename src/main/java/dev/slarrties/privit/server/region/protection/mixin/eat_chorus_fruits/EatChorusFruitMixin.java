package dev.slarrties.privit.server.region.protection.mixin.eat_chorus_fruits;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.consume_effects.TeleportRandomlyConsumeEffect;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

@AssociatedRule(Rule.EAT_CHORUS_FRUITS)
@Mixin(TeleportRandomlyConsumeEffect.class)
public abstract class EatChorusFruitMixin {

    @Unique
    private static final ThreadLocal<Boolean> HAS_NOTIFIED = ThreadLocal.withInitial(() -> false);

    @Inject(method = "apply", at = @At("HEAD"))
    private void resetNotificationFlag(Level level, ItemStack stack, LivingEntity user, CallbackInfoReturnable<Boolean> cir) {
        HAS_NOTIFIED.set(false);
    }

    @WrapOperation(
            method = "apply",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;randomTeleport(DDDZ)Z"
            )
    )
    private boolean preventTeleportToProtectedRegion(LivingEntity user, double x, double y, double z, boolean particle, Operation<Boolean> original) {
        if (user instanceof ServerPlayer serverPlayer && user.level() instanceof ServerLevel serverLevel) {
            BlockPos targetPos = BlockPos.containing(x, y, z);

            if (!RegionPermissionChecker.isAllowed(serverPlayer.getUUID(), Rule.EAT_CHORUS_FRUITS, targetPos, serverLevel)) {
                if (!HAS_NOTIFIED.get()) {
                    PlayerNotification.trySend(serverPlayer, NotificationType.DENY_EAT_CHORUS_FRUIT, Color.RED);
                    HAS_NOTIFIED.set(true);
                }
                return false;
            }
        }

        return original.call(user, x, y, z, particle);
    }
}