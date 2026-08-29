package dev.slarrties.privit.server.region.protection.mixin.redstone;

import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.tracking.protection.RedstoneOriginTracker;
import dev.slarrties.privit.server.tracking.redstone.RedstoneSourceRegistry;

import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public abstract class RedstoneBlockItemPlacementMixin {

    @Shadow
    @Nullable
    protected abstract BlockState getPlacementState(ItemPlacementContext context);

    @Inject(
            method = "place(Lnet/minecraft/item/ItemPlacementContext;)Lnet/minecraft/util/ActionResult;",
            at = @At("HEAD")
    )
    private void onBlockItemPlaceEarly(ItemPlacementContext context, CallbackInfoReturnable<ActionResult> cir) {
        if (context.getWorld().isClient()) return;
        if (!(context.getWorld() instanceof ServerWorld serverWorld)) return;
        if (!(context.getPlayer() instanceof ServerPlayerEntity serverPlayer)) return;

        BlockState placementState = getPlacementState(context);
        if (placementState == null) return;

        BlockPos pos = context.getBlockPos();
        if (!RedstoneSourceRegistry.isSource(placementState, serverWorld, pos)) return;

        RedstoneOriginTracker redstoneOriginTracker = WorldRegistry.get(serverWorld).getTrackerManager().getRedstoneOriginTracker();
        redstoneOriginTracker.record(pos, serverPlayer.getUuid());
    }
}