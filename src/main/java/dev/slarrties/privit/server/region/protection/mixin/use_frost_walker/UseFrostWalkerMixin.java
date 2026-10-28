package dev.slarrties.privit.server.region.protection.mixin.use_frost_walker;

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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.enchantment.effects.ReplaceDisk;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

@AssociatedRule(Rule.USE_FROST_WALKER)
@Mixin(ReplaceDisk.class)
public abstract class UseFrostWalkerMixin {

    @WrapOperation(
            method = "apply",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerLevel;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"
            )
    )
    private boolean preventFrostWalkerFreeze(
            ServerLevel serverLevel, BlockPos pos, BlockState blockState,
            Operation<Boolean> original,
            @Local(argsOnly = true) Entity entity) {
        if (entity instanceof ServerPlayer serverPlayer) {
            if (!RegionPermissionChecker.isAllowed(serverPlayer.getUUID(), Rule.USE_FROST_WALKER, pos, serverLevel)) {
                PlayerNotification.trySend(serverPlayer, NotificationType.DENY_USE_FROST_WALKER, Color.RED);
                return false;
            }
        }

        return original.call(serverLevel, pos, blockState);
    }
}