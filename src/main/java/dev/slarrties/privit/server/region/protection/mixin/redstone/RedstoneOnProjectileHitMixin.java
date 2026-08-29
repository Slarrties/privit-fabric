package dev.slarrties.privit.server.region.protection.mixin.redstone;

import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.tracking.protection.RedstoneOriginTracker;

import net.minecraft.world.World;
import net.minecraft.block.BlockState;
import net.minecraft.block.TargetBlock;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TargetBlock.class)
public abstract class RedstoneOnProjectileHitMixin {

    @Inject(method = "onProjectileHit", at = @At("HEAD"))
    private void onProjectileHit(World world, BlockState state, BlockHitResult hitResult,
                                 ProjectileEntity projectile, CallbackInfo ci) {
        if (!(world instanceof ServerWorld serverWorld)) return;
        if (!(projectile.getOwner() instanceof ServerPlayerEntity serverPlayer)) return;

        RedstoneOriginTracker redstoneOriginTracker = WorldRegistry.get(serverWorld)
                .getTrackerManager()
                .getRedstoneOriginTracker();
        redstoneOriginTracker.record(hitResult.getBlockPos(), serverPlayer.getUuid());
    }
}