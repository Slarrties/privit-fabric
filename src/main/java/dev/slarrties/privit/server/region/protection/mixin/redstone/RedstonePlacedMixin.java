package dev.slarrties.privit.server.region.protection.mixin.redstone;

import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.tracking.protection.RedstoneOriginTracker;
import dev.slarrties.privit.server.tracking.redstone.RedstoneSourceRegistry;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.world.World;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Block.class)
public abstract class RedstonePlacedMixin {

    @Inject(method = "onPlaced", at = @At("HEAD"))
    private void onPlaced(World world, BlockPos pos, BlockState state,
                          LivingEntity placer, ItemStack stack, CallbackInfo ci) {
        if (!(world instanceof ServerWorld serverWorld)) return;
        if (!(placer instanceof ServerPlayerEntity player)) return;
        if (!RedstoneSourceRegistry.isSource(state, world, pos)) return;

        RedstoneOriginTracker redstoneTracker = WorldRegistry.get(serverWorld)
                .getTrackerManager()
                .getRedstoneOriginTracker();
        redstoneTracker.record(pos, player.getUuid());
    }
}