package dev.slarrties.privit.server.region.protection.mixin.spread_sculk;

import com.llamalad7.mixinextras.sugar.Local;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.tracking.context.SculkBloomContext;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.SculkSpreader;
import net.minecraft.world.level.block.SculkVeinBlock;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@AssociatedRule(Rule.SPREAD_SCULK)
@Mixin(SculkVeinBlock.class)
public abstract class LichenGrowerMixin {

    @Inject(
            method = "attemptPlaceSculk",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/LevelAccessor;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"
            ),
            cancellable = true
    )
    private void preventSculkPlacement(
            SculkSpreader spreader,
            LevelAccessor level,
            BlockPos pos,
            RandomSource random,
            CallbackInfoReturnable<Boolean> cir,
            @Local(name = "supportPos") BlockPos supportPos
    ) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        UUID responsible = SculkBloomContext.getResponsible();
        if (responsible == null) return;
        if (!RegionPermissionChecker.isAllowed(responsible, Rule.SPREAD_SCULK, supportPos, serverLevel)) {
            cir.setReturnValue(false);
        }
    }
}