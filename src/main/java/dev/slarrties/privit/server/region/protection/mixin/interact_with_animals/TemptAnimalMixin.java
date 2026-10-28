package dev.slarrties.privit.server.region.protection.mixin.interact_with_animals;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;
import dev.slarrties.privit.server.tracking.protection.InfluencedEntityTracker;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@AssociatedRule(Rule.INTERACT_WITH_ANIMALS)
@Mixin(TemptGoal.class)
public abstract class TemptAnimalMixin {

    @Shadow @Final
    protected Mob mob;

    @Shadow
    protected Player player;

    @Inject(method = "canUse", at = @At("RETURN"), cancellable = true)
    private void handleTempt(CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) return;
        if (this.mob.level().isClientSide()) return;

        Player player = this.player;
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        BlockPos checkPos = this.mob.blockPosition();
        boolean allowed = RegionPermissionChecker.isAllowed(serverPlayer.getUUID(), Rule.INTERACT_WITH_ANIMALS, checkPos, serverPlayer.level());
        if (!allowed) {
            cir.setReturnValue(false);
            return;
        }

        if (this.mob.level() instanceof ServerLevel serverLevel) {
            InfluencedEntityTracker tracker = WorldRegistry.get(serverLevel)
                    .getTrackerManager()
                    .getInfluencedEntityTracker();
            tracker.record(this.mob, serverPlayer.getUUID());
        }
    }
}