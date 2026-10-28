package dev.slarrties.privit.server.region.protection.mixin.redstone;

import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.tracking.protection.RedstoneOriginTracker;
import dev.slarrties.privit.server.tracking.redstone.RedstoneSourceRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ButtonBlock.class)
public abstract class RedstoneButtonPressMixin {

    @Inject(method = "press", at = @At("HEAD"))
    private void onButtonPress(BlockState state, Level level, BlockPos pos, @Nullable Player player, CallbackInfo ci) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        if (!RedstoneSourceRegistry.isSource(state, serverLevel, pos)) return;

        RedstoneOriginTracker redstoneTracker = WorldRegistry.get(serverLevel)
                .getTrackerManager()
                .getRedstoneOriginTracker();
        redstoneTracker.record(pos, serverPlayer.getUUID());
    }
}