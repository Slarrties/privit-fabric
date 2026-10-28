package dev.slarrties.privit.server.region.protection.mixin.redstone;

import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.tracking.protection.RedstoneOriginTracker;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.block.TargetBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TargetBlock.class)
public abstract class RedstoneOnProjectileHitMixin {

    @Inject(method = "onProjectileHit", at = @At("HEAD"))
    private void onProjectileHit(Level level, BlockState state, BlockHitResult hitResult,
                                 Projectile projectile, CallbackInfo ci) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        if (!(projectile.getOwner() instanceof ServerPlayer serverPlayer)) return;

        RedstoneOriginTracker redstoneOriginTracker = WorldRegistry.get(serverLevel)
                .getTrackerManager()
                .getRedstoneOriginTracker();
        redstoneOriginTracker.record(hitResult.getBlockPos(), serverPlayer.getUUID());
    }
}