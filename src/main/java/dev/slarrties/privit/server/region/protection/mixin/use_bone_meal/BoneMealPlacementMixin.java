package dev.slarrties.privit.server.region.protection.mixin.use_bone_meal;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;
import dev.slarrties.privit.server.tracking.context.BoneMealUseContext;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@AssociatedRule(Rule.USE_BONE_MEAL)
@Mixin(Level.class)
public abstract class BoneMealPlacementMixin {

    @Inject(
            method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void checkBoneMealPlacement(BlockPos pos, BlockState state, int flags, CallbackInfoReturnable<Boolean> cir) {
        Level level = (Level) (Object) this;
        if (level.isClientSide()) return;

        BoneMealUseContext context = BoneMealUseContext.getCurrent();
        if (context == null || context.getResponsible() == null) return;
        if (!(level instanceof ServerLevel serverLevel)) return;
        if (RegionPermissionChecker.isAllowed(context.getResponsible(), Rule.USE_BONE_MEAL, pos, serverLevel)) return;
        cir.setReturnValue(false);
    }
}