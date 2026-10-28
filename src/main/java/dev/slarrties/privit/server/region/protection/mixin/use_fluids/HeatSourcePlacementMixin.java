package dev.slarrties.privit.server.region.protection.mixin.use_fluids;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.util.HeatSources;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.tracking.protection.CampfireOriginTracker;
import dev.slarrties.privit.server.tracking.protection.HeatSourceOriginTracker;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@AssociatedRule(Rule.USE_FLUIDS)
@Mixin(BlockItem.class)
public abstract class HeatSourcePlacementMixin {

    @Inject(
            method = "place(Lnet/minecraft/world/item/context/BlockPlaceContext;)Lnet/minecraft/world/InteractionResult;",
            at = @At("RETURN")
    )
    private void onHeatSourcePlaced(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
        InteractionResult result = cir.getReturnValue();
        if (result != InteractionResult.SUCCESS && result != InteractionResult.CONSUME) return;

        Level level = context.getLevel();
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) return;

        BlockPos pos = context.getClickedPos();
        BlockState state = serverLevel.getBlockState(pos);
        Block block = state.getBlock();

        if (!HeatSources.isHeatSource(block)) return;
        if (!(context.getPlayer() instanceof ServerPlayer serverPlayer)) return;
        if (state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE)) {
            if (state.hasProperty(BlockStateProperties.LIT) && state.getValue(BlockStateProperties.LIT)) {
                CampfireOriginTracker campfireTracker = WorldRegistry.get(serverLevel)
                        .getTrackerManager()
                        .getCampfireOriginTracker();
                campfireTracker.record(pos, serverPlayer.getUUID());
            }
        }

        HeatSourceOriginTracker heatSourceTracker = WorldRegistry.get(serverLevel)
                .getTrackerManager()
                .getHeatSourceOriginTracker();
        heatSourceTracker.record(pos, serverPlayer.getUUID());
    }
}