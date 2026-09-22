package dev.slarrties.privit.server.region.protection.mixin.trigger_raids;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;
import dev.slarrties.privit.server.tracking.protection.InfluencedEntityTracker;

import net.minecraft.world.World;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.RavagerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.server.world.ServerWorld;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import java.util.UUID;

@AssociatedRule(Rule.TRIGGER_RAIDS)
@Mixin(RavagerEntity.class)
public abstract class RavagerGriefingMixin {

    @WrapOperation(
            method = "tickMovement",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;breakBlock(Lnet/minecraft/util/math/BlockPos;ZLnet/minecraft/entity/Entity;)Z"
            )
    )
    private boolean wrapRavagerBlockBreaking(World world, BlockPos pos, boolean drop, Entity entity, Operation<Boolean> original) {
        if (world instanceof ServerWorld serverWorld && entity instanceof RavagerEntity ravager) {
            InfluencedEntityTracker influencedTracker = WorldRegistry.get(serverWorld)
                    .getTrackerManager()
                    .getInfluencedEntityTracker();
            UUID triggerUuid = influencedTracker.getResponsible(ravager);

            if (triggerUuid != null) {
                boolean allowed = RegionPermissionChecker.isAllowed(triggerUuid, Rule.TRIGGER_RAIDS, pos, serverWorld);
                if (!allowed) return false;
            }
        }

        return original.call(world, pos, drop, entity);
    }
}