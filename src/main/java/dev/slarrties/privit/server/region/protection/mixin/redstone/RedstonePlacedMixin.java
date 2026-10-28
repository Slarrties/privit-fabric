package dev.slarrties.privit.server.region.protection.mixin.redstone;

import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.tracking.protection.RedstoneOriginTracker;
import dev.slarrties.privit.server.tracking.redstone.RedstoneSourceRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Block.class)
public abstract class RedstonePlacedMixin {

    @Inject(method = "setPlacedBy", at = @At("HEAD"))
    private void onPlaced(Level level, BlockPos pos, BlockState state,
                          LivingEntity placer, ItemStack stack, CallbackInfo ci) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        if (!(placer instanceof ServerPlayer player)) return;
        if (!RedstoneSourceRegistry.isSource(state, level, pos)) return;

        RedstoneOriginTracker redstoneTracker = WorldRegistry.get(serverLevel)
                .getTrackerManager()
                .getRedstoneOriginTracker();
        redstoneTracker.record(pos, player.getUUID());
    }
}