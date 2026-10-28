package dev.slarrties.privit.server.region.protection.mixin.use_fluids;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.tracking.protection.IceOriginTracker;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@AssociatedRule(Rule.USE_FLUIDS)
@Mixin(BlockItem.class)
public abstract class IcePlacementMixin {

    @Inject(
            method = "place(Lnet/minecraft/world/item/context/BlockPlaceContext;)Lnet/minecraft/world/InteractionResult;",
            at = @At("RETURN")
    )
    private void onIcePlaced(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
        InteractionResult result = cir.getReturnValue();
        if (result != InteractionResult.SUCCESS && result != InteractionResult.CONSUME) return;

        Level level = context.getLevel();
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) return;

        BlockPos pos = context.getClickedPos();
        BlockState state = serverLevel.getBlockState(pos);

        if (!state.is(Blocks.ICE)) return;
        if (!(context.getPlayer() instanceof ServerPlayer serverPlayer)) return;

        IceOriginTracker iceOriginTracker = WorldRegistry.get(serverLevel)
                .getTrackerManager()
                .getIceOriginTracker();
        iceOriginTracker.record(pos, serverPlayer.getUUID());
    }
}