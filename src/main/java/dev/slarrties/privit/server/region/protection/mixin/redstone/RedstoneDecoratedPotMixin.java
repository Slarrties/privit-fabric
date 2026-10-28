package dev.slarrties.privit.server.region.protection.mixin.redstone;

import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.tracking.protection.InfluencedEntityTracker;
import dev.slarrties.privit.server.tracking.protection.RedstoneOriginTracker;
import dev.slarrties.privit.server.tracking.redstone.RedstoneSourceRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DecoratedPotBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(DecoratedPotBlock.class)
public abstract class RedstoneDecoratedPotMixin {

    @Inject(method = "playerWillDestroy", at = @At("HEAD"))
    private void onPlayerWillDestroy(Level level, BlockPos pos, BlockState state, Player player, CallbackInfoReturnable<BlockState> cir) {
        checkResponsobility(state, level, pos, player);
    }

    @Inject(method = "onProjectileHit", at = @At("HEAD"))
    private void onProjectileHit(Level level, BlockState state, BlockHitResult hitResult, Projectile projectile, CallbackInfo ci) {
        checkResponsobility(state, level, projectile.getOnPos(), projectile);
    }

    @Unique
    private void checkResponsobility(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        if (!RedstoneSourceRegistry.isSource(state, serverLevel, pos)) return;

        UUID responsible = null;

        if (entity instanceof ServerPlayer serverPlayer) {
            responsible = serverPlayer.getUUID();
        } else if (entity != null) {
            InfluencedEntityTracker influencedTracker = WorldRegistry.get(serverLevel)
                    .getTrackerManager()
                    .getInfluencedEntityTracker();
            responsible = influencedTracker.getResponsible(entity);
        }

        if (responsible != null) {
            RedstoneOriginTracker redstoneTracker = WorldRegistry.get(serverLevel)
                    .getTrackerManager()
                    .getRedstoneOriginTracker();
            redstoneTracker.record(pos, responsible);
        }
    }
}