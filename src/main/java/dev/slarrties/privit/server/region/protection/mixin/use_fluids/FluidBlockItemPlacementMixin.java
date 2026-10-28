package dev.slarrties.privit.server.region.protection.mixin.use_fluids;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.tracking.protection.FluidOriginTracker;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@AssociatedRule(Rule.USE_FLUIDS)
@Mixin(BlockItem.class)
public abstract class FluidBlockItemPlacementMixin {

    @Inject(
            method = "place(Lnet/minecraft/world/item/context/BlockPlaceContext;)Lnet/minecraft/world/InteractionResult;",
            at = @At("RETURN")
    )
    private void onBlockPlaced(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (context.getLevel().isClientSide() || !(context.getLevel() instanceof ServerLevel serverLevel)) return;

        BlockPos pos = context.getClickedPos();
        FluidState fluidState = serverLevel.getFluidState(pos);
        BlockState placedState = serverLevel.getBlockState(pos);

        FluidOriginTracker fluidTracker = WorldRegistry.get(serverLevel)
                .getTrackerManager()
                .getFluidOriginTracker();

        if (fluidState.isEmpty() && !isFluidPermeable(placedState, serverLevel, pos)) {
            fluidTracker.remove(pos);
        }
    }

    @Unique
    private static boolean isFluidPermeable(BlockState state, ServerLevel level, BlockPos pos) {
        if (state.getBlock() instanceof LiquidBlock) { // Waterlogged
            return true;
        }

        return !state.isSolid() || state.is(BlockTags.REPLACEABLE);
    }
}