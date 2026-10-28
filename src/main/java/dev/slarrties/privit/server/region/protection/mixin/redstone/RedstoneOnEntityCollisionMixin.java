package dev.slarrties.privit.server.region.protection.mixin.redstone;

import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.tracking.protection.RedstoneOriginTracker;
import dev.slarrties.privit.server.tracking.redstone.RedstoneSourceRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.BasePressurePlateBlock;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// TODO: call is too frequent when pressed. Optimization is needed.
@Mixin(BasePressurePlateBlock.class)
public abstract class RedstoneOnEntityCollisionMixin {

    @Inject(method = "entityInside", at = @At("HEAD"))
    private void onPressurePlateActivated(BlockState state, Level level, BlockPos pos, Entity entity,
                                          InsideBlockEffectApplier effectApplier, boolean isPrecise,
                                          CallbackInfo ci) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        if (!(entity instanceof ServerPlayer serverPlayer)) return;
        if (!RedstoneSourceRegistry.isSource(state, serverLevel, pos)) return;

        RedstoneOriginTracker redstoneOriginTracker = WorldRegistry.get(serverLevel)
                .getTrackerManager()
                .getRedstoneOriginTracker();
        redstoneOriginTracker.record(pos, serverPlayer.getUUID());
    }
}