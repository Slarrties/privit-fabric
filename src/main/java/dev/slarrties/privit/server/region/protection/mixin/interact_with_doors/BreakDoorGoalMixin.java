package dev.slarrties.privit.server.region.protection.mixin.interact_with_doors;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;
import dev.slarrties.privit.server.tracking.protection.InfluencedEntityTracker;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.BreakDoorGoal;
import net.minecraft.server.level.ServerLevel;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@AssociatedRule(Rule.INTERACT_WITH_DOORS)
@Mixin(BreakDoorGoal.class)
public abstract class BreakDoorGoalMixin {

    @Inject(method = "canUse", at = @At("RETURN"), cancellable = true)
    private void preventBreakingDoor(CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) return;

        DoorInteractGoalAccessor accessor = (DoorInteractGoalAccessor) (Object) this;
        Mob mob = accessor.getMob();
        BlockPos doorPos = accessor.getDoorPos();

        if (mob.level().isClientSide()) return;
        if (!(mob.level() instanceof ServerLevel serverLevel)) return;

        InfluencedEntityTracker tracker = WorldRegistry.get(serverLevel)
                .getTrackerManager()
                .getInfluencedEntityTracker();
        UUID responsible = tracker.getResponsible(mob);
        if (responsible == null) return;

        BlockPos checkPos = (doorPos != null && !doorPos.equals(BlockPos.ZERO))
                ? doorPos
                : mob.blockPosition();

        if (!RegionPermissionChecker.isAllowed(responsible, Rule.INTERACT_WITH_DOORS, checkPos, serverLevel)) {
            cir.setReturnValue(false);
        }
    }
}