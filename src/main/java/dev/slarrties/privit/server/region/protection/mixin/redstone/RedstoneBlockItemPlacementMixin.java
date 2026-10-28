package dev.slarrties.privit.server.region.protection.mixin.redstone;

import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.tracking.protection.RedstoneOriginTracker;
import dev.slarrties.privit.server.tracking.redstone.RedstoneSourceRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public abstract class RedstoneBlockItemPlacementMixin {

    @Shadow @Nullable
    protected abstract BlockState getPlacementState(BlockPlaceContext context);

    @Inject(
            method = "place(Lnet/minecraft/world/item/context/BlockPlaceContext;)Lnet/minecraft/world/InteractionResult;",
            at = @At("HEAD")
    )
    private void onBlockItemPlaceEarly(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (context.getLevel().isClientSide()) return;
        if (!(context.getLevel() instanceof ServerLevel serverLevel)) return;
        if (!(context.getPlayer() instanceof ServerPlayer serverPlayer)) return;

        BlockState placementState = getPlacementState(context);
        if (placementState == null) return;

        BlockPos pos = context.getClickedPos();
        if (!RedstoneSourceRegistry.isSource(placementState, serverLevel, pos)) return;

        RedstoneOriginTracker redstoneOriginTracker = WorldRegistry.get(serverLevel)
                .getTrackerManager()
                .getRedstoneOriginTracker();
        redstoneOriginTracker.record(pos, serverPlayer.getUUID());
    }
}