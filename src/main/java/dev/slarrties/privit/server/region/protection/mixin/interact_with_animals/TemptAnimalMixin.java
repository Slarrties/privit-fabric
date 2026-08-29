package dev.slarrties.privit.server.region.protection.mixin.interact_with_animals;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;
import dev.slarrties.privit.server.tracking.protection.InfluencedEntityTracker;

import net.minecraft.util.math.BlockPos;
import net.minecraft.entity.ai.goal.TemptGoal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@AssociatedRule(Rule.INTERACT_WITH_ANIMALS)
@Mixin(TemptGoal.class)
public abstract class TemptAnimalMixin {

    @Shadow @Final protected PathAwareEntity mob;
    @Shadow protected PlayerEntity closestPlayer;

    @Inject(method = "canStart", at = @At("RETURN"), cancellable = true)
    private void handleTempt(CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) return;
        if (this.mob.getWorld().isClient) return;

        PlayerEntity player = this.closestPlayer;
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;

        BlockPos checkPos = this.mob.getBlockPos();
        boolean allowed = RegionPermissionChecker.isAllowed(serverPlayer, Rule.INTERACT_WITH_ANIMALS, checkPos);

        if (!allowed) {
            cir.setReturnValue(false);
            return;
        }

        if (this.mob.getWorld() instanceof ServerWorld serverWorld) {
            InfluencedEntityTracker tracker = WorldRegistry.get(serverWorld)
                    .getTrackerManager()
                    .getInfluencedEntityTracker();

            tracker.record(this.mob, serverPlayer.getUuid());
        }
    }
}