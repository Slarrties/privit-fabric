package dev.slarrties.privit.server.region.protection.mixin.use_frost_walker;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;
import dev.slarrties.privit.server.tracking.protection.InfluencedEntityTracker;

import net.minecraft.world.World;
import net.minecraft.block.Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import java.util.UUID;

@AssociatedRule(Rule.USE_FROST_WALKER)
@Mixin(net.minecraft.enchantment.FrostWalkerEnchantment.class)
public abstract class UseFrostWalkerMixin {

    @WrapOperation(
            method = "freezeWater",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)Z"
            )
    )
    private static boolean preventFrostWalkerFreeze(World world, BlockPos pos, BlockState newState,
                                                    Operation<Boolean> original, @Local(argsOnly = true) LivingEntity user) {
        if (!newState.isOf(Blocks.FROSTED_ICE)) return original.call(world, pos, newState);
        if (!(world instanceof ServerWorld serverWorld)) return original.call(world, pos, newState);

        UUID responsibleUuid = null;
        boolean isDirectPlayer = false;

        if (user instanceof ServerPlayerEntity player) {
            responsibleUuid = player.getUuid();
            isDirectPlayer = true;
        } else {
            InfluencedEntityTracker influencedEntityTracker = WorldRegistry.get(serverWorld)
                    .getTrackerManager()
                    .getInfluencedEntityTracker();
            responsibleUuid = influencedEntityTracker.getResponsible(user);
        }

        if (responsibleUuid == null) return original.call(world, pos, newState);
        if (!RegionPermissionChecker.isAllowed(responsibleUuid, Rule.USE_FROST_WALKER, pos, serverWorld)) {
            if (isDirectPlayer) {
                PlayerNotification.trySend((ServerPlayerEntity) user, NotificationType.DENY_USE_FROST_WALKER, Color.RED);
            }
            return false;
        }

        return original.call(world, pos, newState);
    }
}