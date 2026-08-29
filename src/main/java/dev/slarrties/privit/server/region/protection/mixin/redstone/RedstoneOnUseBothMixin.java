package dev.slarrties.privit.server.region.protection.mixin.redstone;

import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.tracking.protection.RedstoneOriginTracker;
import dev.slarrties.privit.server.tracking.redstone.RedstoneSourceRegistry;

import net.minecraft.block.*;
import net.minecraft.world.World;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.ItemActionResult;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({
        LecternBlock.class,
        JukeboxBlock.class,
        ComposterBlock.class,
        DecoratedPotBlock.class,
        ChiseledBookshelfBlock.class
})
public abstract class RedstoneOnUseBothMixin {

    @Inject(method = "onUse", at = @At("HEAD"))
    private void onComparatorDrivenUse(BlockState state, World world, BlockPos pos,
                                       PlayerEntity player, BlockHitResult hit,
                                       CallbackInfoReturnable<ActionResult> cir) {
        tryRegisterOwner(state, world, pos, player);
    }

    @Inject(method = "onUseWithItem", at = @At("HEAD"))
    private void onComparatorDrivenUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos,
                                               PlayerEntity player, Hand hand, BlockHitResult hit,
                                               CallbackInfoReturnable<ItemActionResult> cir) {
        tryRegisterOwner(state, world, pos, player);
    }

    @Unique
    private void tryRegisterOwner(BlockState state, World world, BlockPos pos, PlayerEntity player) {
        if (!(world instanceof ServerWorld serverWorld)) return;
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;
        if (!RedstoneSourceRegistry.isSource(state, serverWorld, pos)) return;

        RedstoneOriginTracker redstoneTracker = WorldRegistry.get(serverWorld)
                .getTrackerManager()
                .getRedstoneOriginTracker();
        redstoneTracker.record(pos, serverPlayer.getUuid());
    }
}