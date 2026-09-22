package dev.slarrties.privit.server.region.protection.mixin.build;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;
import dev.slarrties.privit.server.tracking.protection.InfluencedEntityTracker;

import net.minecraft.block.BlockState;
import net.minecraft.block.FarmlandBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@AssociatedRule(Rule.BUILD)
@Mixin(FarmlandBlock.class)
public abstract class FarmlandTrampleMixin {

    @Inject(method = "onLandedUpon", at = @At("HEAD"), cancellable = true)
    private void preventFarmlandTrampling(World world, BlockState state, BlockPos pos, Entity entity, float fallDistance, CallbackInfo ci) {
        if (world.isClient || !(world instanceof ServerWorld serverWorld)) return;

        UUID responsibleUuid = null;

        if (entity instanceof PlayerEntity player) {
            responsibleUuid = player.getUuid();
        } else {
            InfluencedEntityTracker influencedEntityTracker = WorldRegistry.get(serverWorld)
                    .getTrackerManager()
                    .getInfluencedEntityTracker();
            responsibleUuid = influencedEntityTracker.getResponsible(entity);
        }

        if (responsibleUuid == null) return;

        boolean allowed = RegionPermissionChecker.isAllowed(responsibleUuid, Rule.BUILD, pos, serverWorld);
        if (!allowed) ci.cancel();
    }
}