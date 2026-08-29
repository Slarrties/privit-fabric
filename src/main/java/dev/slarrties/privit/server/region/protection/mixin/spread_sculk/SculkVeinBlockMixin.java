package dev.slarrties.privit.server.region.protection.mixin.spread_sculk;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.tracking.context.SculkBloomContext;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;

import net.minecraft.world.WorldAccess;
import net.minecraft.block.BlockState;
import net.minecraft.block.SculkVeinBlock;
import net.minecraft.block.entity.SculkSpreadManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.server.world.ServerWorld;

import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;
import java.util.Collection;

@AssociatedRule(Rule.SPREAD_SCULK)
@Mixin(SculkVeinBlock.class)
public abstract class SculkVeinBlockMixin {

    @Inject(method = "place", at = @At("HEAD"), cancellable = true)
    private static void onPlaceVein(WorldAccess world, BlockPos pos, BlockState state,
                                    Collection<Direction> directions, CallbackInfoReturnable<Boolean> cir) {
        if (!shouldBlock(world, pos)) return;

        cir.setReturnValue(false);
    }

    @Inject(
            method = "convertToBlock",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/WorldAccess;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"
            ),
            cancellable = true
    )
    private void onConvertToSculk(SculkSpreadManager spreadManager, WorldAccess world, BlockPos pos,
                                  Random random, CallbackInfoReturnable<Boolean> cir, @Local(ordinal = 1) BlockPos blockPos) {
        if (!shouldBlock(world, blockPos)) return;

        cir.setReturnValue(false);
    }

    @Unique
    private static boolean shouldBlock(WorldAccess world, BlockPos pos) {
        if (!(world instanceof ServerWorld serverWorld)) return false;

        UUID responsible = SculkBloomContext.getResponsible();
        if (responsible == null) return false;

        return !RegionPermissionChecker.isAllowed(responsible, Rule.SPREAD_SCULK, pos, serverWorld);
    }
}