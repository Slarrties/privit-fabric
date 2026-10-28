package dev.slarrties.privit.server.region.protection.mixin.build;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;
import dev.slarrties.privit.server.tracking.protection.InfluencedEntityTracker;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@AssociatedRule(Rule.BUILD)
@Mixin(FarmlandBlock.class)
public abstract class FarmlandTrampleMixin {

    @Inject(method = "fallOn", at = @At("HEAD"), cancellable = true)
    private void preventFarmlandTrampling(Level level, BlockState state, BlockPos pos, Entity entity, double fallDistance, CallbackInfo ci) {
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) return;

        UUID responsibleUuid = null;
        if (entity instanceof Player player) {
            responsibleUuid = player.getUUID();
        } else {
            InfluencedEntityTracker influencedEntityTracker = WorldRegistry.get(serverLevel)
                    .getTrackerManager()
                    .getInfluencedEntityTracker();
            responsibleUuid = influencedEntityTracker.getResponsible(entity);
        }

        if (responsibleUuid == null) return;

        boolean allowed = RegionPermissionChecker.isAllowed(responsibleUuid, Rule.BUILD, pos, serverLevel);
        if (!allowed) ci.cancel();
    }
}