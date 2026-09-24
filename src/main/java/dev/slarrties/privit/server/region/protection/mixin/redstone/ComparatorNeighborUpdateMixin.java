package dev.slarrties.privit.server.region.protection.mixin.redstone;

import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.tracking.protection.RedstoneOriginTracker;
import dev.slarrties.privit.server.tracking.redstone.RedstoneSourceRegistry;

import net.minecraft.world.World;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.AbstractRedstoneGateBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.server.world.ServerWorld;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(AbstractRedstoneGateBlock.class)
public abstract class ComparatorNeighborUpdateMixin {

    @Inject(method = "neighborUpdate", at = @At("HEAD"))
    private void onComparatorNeighborUpdate(BlockState state, World world, BlockPos pos,
                                            Block sourceBlock, BlockPos sourcePos, boolean notify,
                                            CallbackInfo ci) {
        if (!(world instanceof ServerWorld serverWorld)) return;
        if (!RedstoneSourceRegistry.isSource(state, serverWorld, pos)) return;

        RedstoneOriginTracker redstoneOriginTracker = WorldRegistry.get(serverWorld)
                .getTrackerManager()
                .getRedstoneOriginTracker();

        if (redstoneOriginTracker.getResponsible(pos) == null) return;

        UUID responsible = redstoneOriginTracker.getResponsible(sourcePos);
        if (responsible != null) redstoneOriginTracker.record(pos, responsible);
    }
}