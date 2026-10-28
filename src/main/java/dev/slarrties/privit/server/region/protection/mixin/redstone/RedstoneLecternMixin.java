package dev.slarrties.privit.server.region.protection.mixin.redstone;

import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.tracking.redstone.RedstoneSourceRegistry;
import dev.slarrties.privit.server.tracking.protection.InfluencedEntityTracker;
import dev.slarrties.privit.server.tracking.protection.RedstoneOriginTracker;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(LecternBlock.class)
public abstract class RedstoneLecternMixin {

    @Inject(method = "tryPlaceBook", at = @At("HEAD"))
    private static void onTryPlaceBook(LivingEntity sourceEntity, Level level, BlockPos pos, BlockState state,
                                       ItemStack item, CallbackInfoReturnable<Boolean> cir) {
        checkResponsobility(state, level, pos, sourceEntity);
    }

    @Inject(method = "placeBook", at = @At("HEAD"))
    private static void onPlaceBook(LivingEntity sourceEntity, Level level, BlockPos pos, BlockState state, ItemStack book, CallbackInfo ci) {
        checkResponsobility(state, level, pos, sourceEntity);
    }

    @Inject(method = "resetBookState", at = @At("HEAD"))
    private static void onResetBookState(Entity sourceEntity, Level level, BlockPos pos, BlockState state, boolean hasBook, CallbackInfo ci) {
        checkResponsobility(state, level, pos, (LivingEntity) sourceEntity);
    }

    @Unique
    private static void checkResponsobility(BlockState state, Level level, BlockPos pos, LivingEntity entity) {
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