package dev.slarrties.privit.server.region.protection.mixin.interact_with_doors;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;
import dev.slarrties.privit.server.tracking.protection.InfluencedEntityTracker;

import net.minecraft.util.math.BlockPos;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.ai.goal.BreakDoorGoal;
import net.minecraft.server.world.ServerWorld;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@AssociatedRule(Rule.INTERACT_WITH_DOORS)
@Mixin(BreakDoorGoal.class)
public abstract class BreakDoorGoalMixin {

    // TODO: frequent calls from those who try to break down doors.
    // shouldContinue() ???
    // TODO: vindicators not only break down, but also open doors.
    @Inject(method = "canStart", at = @At("RETURN"), cancellable = true)
    private void preventBreakingDoor(CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) return;

        DoorInteractGoalAccessor accessor = (DoorInteractGoalAccessor) (Object) this;
        MobEntity mob = accessor.getMob();
        BlockPos doorPos = accessor.getDoorPos();

        if (mob.getWorld().isClient) return;

        ServerWorld world = (ServerWorld) mob.getWorld();
        InfluencedEntityTracker tracker = WorldRegistry.get(world)
                .getTrackerManager()
                .getInfluencedEntityTracker();

        UUID responsible = tracker.getResponsible(mob);
        if (responsible == null) return;

        BlockPos checkPos = (doorPos != null && !doorPos.equals(BlockPos.ORIGIN))
                ? doorPos
                : mob.getBlockPos();

        if (!RegionPermissionChecker.isAllowed(responsible, Rule.INTERACT_WITH_DOORS, checkPos, world)) {
            cir.setReturnValue(false);
        }
    }
}