package dev.slarrties.privit.server.region.protection.mixin.trigger_raids;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;
import dev.slarrties.privit.server.tracking.protection.InfluencedEntityTracker;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Ravager;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import java.util.UUID;

@AssociatedRule(Rule.TRIGGER_RAIDS)
@Mixin(Ravager.class)
public abstract class RavagerGriefingMixin {

    @WrapOperation(
            method = "aiStep",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerLevel;destroyBlock(Lnet/minecraft/core/BlockPos;ZLnet/minecraft/world/entity/Entity;)Z"
            )
    )
    private boolean wrapRavagerBlockBreaking(ServerLevel serverLevel, BlockPos blockPos, boolean b, Entity entity, Operation<Boolean> original) {
        if (entity instanceof Ravager ravager) {
            InfluencedEntityTracker influencedTracker = WorldRegistry.get(serverLevel)
                    .getTrackerManager()
                    .getInfluencedEntityTracker();
            UUID triggerUuid = influencedTracker.getResponsible(ravager);

            if (triggerUuid != null) {
                boolean allowed = RegionPermissionChecker.isAllowed(triggerUuid, Rule.TRIGGER_RAIDS, blockPos, serverLevel);
                if (!allowed) return false;
            }
        }
        return original.call(serverLevel, blockPos, b, entity);
    }
}