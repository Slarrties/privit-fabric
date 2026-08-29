package dev.slarrties.privit.server.region.protection.mixin.use_fluids;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.tracking.protection.FluidOriginTracker;

import net.minecraft.fluid.FluidState;
import net.minecraft.block.BlockState;
import net.minecraft.block.Waterloggable;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@AssociatedRule(Rule.USE_FLUIDS)
@Mixin(BlockItem.class)
public abstract class FluidBlockItemPlacementMixin {

    @Inject(
            method = "place(Lnet/minecraft/item/ItemPlacementContext;)Lnet/minecraft/util/ActionResult;",
            at = @At("RETURN")
    )
    private void onBlockPlaced(ItemPlacementContext context, CallbackInfoReturnable<ActionResult> cir) {
        if (context.getWorld().isClient || !(context.getWorld() instanceof ServerWorld serverWorld)) return;

        BlockPos pos = context.getBlockPos();
        FluidState fluidState = serverWorld.getFluidState(pos);
        BlockState placedState = serverWorld.getBlockState(pos);
        FluidOriginTracker fluidTracker = WorldRegistry.get(serverWorld)
                .getTrackerManager()
                .getFluidOriginTracker();

        if (fluidState.isEmpty() && !isFluidPermeable(placedState, serverWorld, pos)) {
            fluidTracker.remove(pos);
        }
    }

    @Unique
    private static boolean isFluidPermeable(BlockState state, ServerWorld world, BlockPos pos) {
        if (state.getBlock() instanceof Waterloggable) {
            return true;
        }

        return state.isTransparent(world, pos) || state.isIn(net.minecraft.registry.tag.BlockTags.REPLACEABLE);
    }
}