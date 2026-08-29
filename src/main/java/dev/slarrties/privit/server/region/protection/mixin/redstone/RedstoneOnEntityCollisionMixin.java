package dev.slarrties.privit.server.region.protection.mixin.redstone;

import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.tracking.protection.RedstoneOriginTracker;
import dev.slarrties.privit.server.tracking.redstone.RedstoneSourceRegistry;

import net.minecraft.world.World;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.BlockState;
import net.minecraft.block.AbstractPressurePlateBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// TODO: call is too frequent when pressed. Optimization is needed.
@Mixin(AbstractPressurePlateBlock.class)
public abstract class RedstoneOnEntityCollisionMixin {

    @Inject(method = "onEntityCollision", at = @At("HEAD"))
    private void onPressurePlateActivated(BlockState state, World world, BlockPos pos, Entity entity, CallbackInfo ci) {
        if (!(world instanceof ServerWorld serverWorld)) return;
        if (!(entity instanceof ServerPlayerEntity serverPlayer)) return;
        if (!RedstoneSourceRegistry.isSource(state, world, pos)) return;

        RedstoneOriginTracker redstoneOriginTracker = WorldRegistry.get(serverWorld)
                .getTrackerManager()
                .getRedstoneOriginTracker();
        redstoneOriginTracker.record(pos, serverPlayer.getUuid());
    }
}