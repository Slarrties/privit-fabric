package dev.slarrties.privit.server.region.protection.mixin.use_fluids;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.util.HeatSources;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.tracking.protection.CampfireOriginTracker;
import dev.slarrties.privit.server.tracking.protection.HeatSourceOriginTracker;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.world.World;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.property.Properties;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@AssociatedRule(Rule.USE_FLUIDS)
@Mixin(BlockItem.class)
public abstract class HeatSourcePlacementMixin {

    @Inject(
            method = "place(Lnet/minecraft/item/ItemPlacementContext;)Lnet/minecraft/util/ActionResult;",
            at = @At("RETURN")
    )
    private void onHeatSourcePlaced(ItemPlacementContext context, CallbackInfoReturnable<ActionResult> cir) {
        if (cir.getReturnValue() != ActionResult.SUCCESS && cir.getReturnValue() != ActionResult.CONSUME) return;
        World world = context.getWorld();
        if (world.isClient || !(world instanceof ServerWorld serverWorld)) return;

        BlockPos pos = context.getBlockPos();
        BlockState state = serverWorld.getBlockState(pos);
        Block block = state.getBlock();

        if (!HeatSources.isHeatSource(block)) return;
        if (!(context.getPlayer() instanceof ServerPlayerEntity serverPlayer)) return;
        if (state.isOf(Blocks.CAMPFIRE) || state.isOf(Blocks.SOUL_CAMPFIRE)) {
            if (state.contains(Properties.LIT) && state.get(Properties.LIT)) {
                CampfireOriginTracker campfireTracker = WorldRegistry.get(serverWorld)
                        .getTrackerManager()
                        .getCampfireOriginTracker();
                campfireTracker.record(pos, serverPlayer.getUuid());
            }
        }

        HeatSourceOriginTracker heatSourceTracker = WorldRegistry.get(serverWorld)
                .getTrackerManager()
                .getHeatSourceOriginTracker();
        heatSourceTracker.record(pos, serverPlayer.getUuid());
    }
}