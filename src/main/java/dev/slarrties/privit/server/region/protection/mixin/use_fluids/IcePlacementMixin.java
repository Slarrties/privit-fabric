package dev.slarrties.privit.server.region.protection.mixin.use_fluids;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.tracking.protection.IceOriginTracker;

import net.minecraft.world.World;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@AssociatedRule(Rule.USE_FLUIDS)
@Mixin(BlockItem.class)
public abstract class IcePlacementMixin {

    @Inject(
            method = "place(Lnet/minecraft/item/ItemPlacementContext;)Lnet/minecraft/util/ActionResult;",
            at = @At("RETURN")
    )
    private void onIcePlaced(ItemPlacementContext context, CallbackInfoReturnable<ActionResult> cir) {
        if (cir.getReturnValue() != ActionResult.SUCCESS && cir.getReturnValue() != ActionResult.CONSUME) return;

        World world = context.getWorld();
        if (world.isClient || !(world instanceof ServerWorld serverWorld)) return;

        BlockPos pos = context.getBlockPos();
        BlockState state = serverWorld.getBlockState(pos);

        if (!state.isOf(Blocks.ICE)) return;
        if (!(context.getPlayer() instanceof ServerPlayerEntity serverPlayer)) return;

        IceOriginTracker iceOriginTracker = WorldRegistry.get(serverWorld)
                .getTrackerManager()
                .getIceOriginTracker();
        iceOriginTracker.record(pos, serverPlayer.getUuid());
    }
}