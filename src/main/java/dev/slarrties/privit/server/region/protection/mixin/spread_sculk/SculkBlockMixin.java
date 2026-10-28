package dev.slarrties.privit.server.region.protection.mixin.spread_sculk;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.tracking.context.SculkBloomContext;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.SculkBlock;
import net.minecraft.world.level.block.SculkSpreader;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@AssociatedRule(Rule.SPREAD_SCULK)
@Mixin(SculkBlock.class)
public abstract class SculkBlockMixin {

    @Inject(
            method = "attemptUseCharge",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/LevelAccessor;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"
            ),
            cancellable = true
    )
    private void onPlaceExtraBlock(
            SculkSpreader.ChargeCursor cursor,
            LevelAccessor level,
            BlockPos originPos,
            RandomSource random,
            SculkSpreader spreader,
            boolean spreadVein,
            CallbackInfoReturnable<Integer> cir
    ) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        UUID responsible = SculkBloomContext.getResponsible();
        if (responsible == null) return;
        BlockPos growthPlacement = cursor.getPos().above();

        if (!RegionPermissionChecker.isAllowed(responsible, Rule.SPREAD_SCULK, growthPlacement, serverLevel)) {
            int charge = cursor.getCharge();
            int xpPerGrowthSpawn = spreader.growthSpawnCost();
            cir.setReturnValue(Math.max(0, charge - xpPerGrowthSpawn));
        }
    }
}