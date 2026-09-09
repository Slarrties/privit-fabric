package dev.slarrties.privit.server.region.protection.mixin.use_bone_meal;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;
import dev.slarrties.privit.server.tracking.context.BoneMealUseContext;

import net.minecraft.world.World;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.server.world.ServerWorld;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@AssociatedRule(Rule.USE_BONE_MEAL)
@Mixin(World.class)
public abstract class BoneMealPlacementMixin {

    @Inject(
            method = "setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void checkBoneMealPlacement(BlockPos pos, BlockState state, int flags, CallbackInfoReturnable<Boolean> cir) {
        World world = (World) (Object) this;
        if (world.isClient()) return;

        BoneMealUseContext context = BoneMealUseContext.getCurrent();
        if (context == null || context.getResponsible() == null) return;
        if (!(world instanceof ServerWorld serverWorld)) return;
        if (RegionPermissionChecker.isAllowed(context.getResponsible(), Rule.USE_BONE_MEAL, pos, serverWorld)) {
            return;
        }

        cir.setReturnValue(false);
    }
}