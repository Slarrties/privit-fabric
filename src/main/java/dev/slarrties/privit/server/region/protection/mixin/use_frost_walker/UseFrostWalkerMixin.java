package dev.slarrties.privit.server.region.protection.mixin.use_frost_walker;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;

import net.minecraft.entity.Entity;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.enchantment.effect.entity.ReplaceDiskEnchantmentEffect;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@AssociatedRule(Rule.USE_FROST_WALKER)
@Mixin(ReplaceDiskEnchantmentEffect.class)
public abstract class UseFrostWalkerMixin {

    @WrapOperation(
            method = "apply(Lnet/minecraft/server/world/ServerWorld;ILnet/minecraft/enchantment/EnchantmentEffectContext;Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/Vec3d;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/world/ServerWorld;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)Z"
            )
    )
    private boolean preventFrostWalkerFreeze(ServerWorld world, BlockPos pos, BlockState newState,
                                             Operation<Boolean> original, @Local(argsOnly = true) Entity user) {
        if (!(user instanceof ServerPlayerEntity serverPlayer)) return original.call(world, pos, newState);
        if (RegionPermissionChecker.isAllowed(serverPlayer.getUuid(), Rule.USE_FROST_WALKER, pos, world)) {
            return original.call(world, pos, newState);
        }

        PlayerNotification.trySend(serverPlayer, NotificationType.DENY_USE_FROST_WALKER, Color.RED);
        return false;
    }
}