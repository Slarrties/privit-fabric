package dev.slarrties.privit.server.region.protection.mixin.spread_sculk;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.tracking.context.SculkBloomContext;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldAccess;
import net.minecraft.block.LichenGrower;
import net.minecraft.server.world.ServerWorld;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;
import java.util.Optional;

@AssociatedRule(Rule.SPREAD_SCULK)
@Mixin(LichenGrower.class)
public abstract class LichenGrowerMixin {

    @Inject(method = "place", at = @At("HEAD"), cancellable = true)
    private void privit$onLichenPlace(WorldAccess world, LichenGrower.GrowPos growPos,
                                      boolean markForPostProcessing, CallbackInfoReturnable<Optional<?>> cir) {
        if (!(world instanceof ServerWorld serverWorld)) return;

        UUID responsible = SculkBloomContext.getResponsible();
        if (responsible == null) return;

        BlockPos pos = growPos.pos();
        boolean allowed = RegionPermissionChecker.isAllowed(responsible, Rule.SPREAD_SCULK, pos, serverWorld);
        if (!allowed) cir.setReturnValue(Optional.empty());
    }
}