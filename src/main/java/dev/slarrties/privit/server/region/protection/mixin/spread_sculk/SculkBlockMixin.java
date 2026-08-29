package dev.slarrties.privit.server.region.protection.mixin.spread_sculk;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.tracking.context.SculkCursorDuck;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;

import net.minecraft.world.WorldAccess;
import net.minecraft.block.SculkBlock;
import net.minecraft.block.entity.SculkSpreadManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.server.world.ServerWorld;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.llamalad7.mixinextras.sugar.Local;

import java.util.UUID;

@AssociatedRule(Rule.SPREAD_SCULK)
@Mixin(SculkBlock.class)
public abstract class SculkBlockMixin {

    @Inject(
            method = "spread",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/WorldAccess;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"
            ),
            cancellable = true
    )
    private void onPlaceExtraBlock(
            SculkSpreadManager.Cursor cursor,
            WorldAccess world,
            BlockPos catalystPos,
            Random random,
            SculkSpreadManager spreadManager,
            boolean shouldConvertToBlock,
            CallbackInfoReturnable<Integer> cir,
            @Local(ordinal = 1) BlockPos blockPos2
    ) {
        if (!(world instanceof ServerWorld serverWorld)) return;
        if (!(cursor instanceof SculkCursorDuck duck)) return;

        UUID responsible = duck.getResponsible();
        if (responsible == null) return;

        boolean allowed = RegionPermissionChecker.isAllowed(responsible, Rule.SPREAD_SCULK, blockPos2, serverWorld);

        if (!allowed) {
            cir.setReturnValue(Math.max(0, cursor.getCharge() - spreadManager.getExtraBlockChance()));
        }
    }
}