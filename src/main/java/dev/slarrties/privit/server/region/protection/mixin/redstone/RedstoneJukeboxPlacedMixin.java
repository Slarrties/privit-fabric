package dev.slarrties.privit.server.region.protection.mixin.redstone;

import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.tracking.protection.InfluencedEntityTracker;
import dev.slarrties.privit.server.tracking.protection.RedstoneOriginTracker;
import dev.slarrties.privit.server.tracking.redstone.RedstoneSourceRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(JukeboxBlock.class)
public abstract class RedstoneJukeboxPlacedMixin {

    @Inject(method = "setPlacedBy", at = @At("HEAD"))
    private void onJukeboxPlaced(Level level, BlockPos pos, BlockState state,
                                 @Nullable LivingEntity placer, ItemStack stack, CallbackInfo ci) {
        checkResponsobility(state, level, pos, placer);
    }

    @Unique
    private void checkResponsobility(BlockState state, Level level, BlockPos pos, LivingEntity entity) {
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