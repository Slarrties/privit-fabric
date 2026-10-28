package dev.slarrties.privit.server.region.protection.mixin.redstone;

import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.tracking.protection.InfluencedEntityTracker;
import dev.slarrties.privit.server.tracking.protection.RedstoneOriginTracker;
import dev.slarrties.privit.server.tracking.redstone.RedstoneSourceRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(ComposterBlock.class)
public abstract class RedstoneComposterMixin {

    @Inject(method = "insertItem", at = @At("HEAD"))
    private static void onInsertItem(Entity entity, BlockState state, ServerLevel level, ItemStack itemStack,
                                     BlockPos pos, CallbackInfoReturnable<BlockState> cir) {
            checkResponsobility(state, entity.level(), pos, entity);
    }

    @Inject(method = "extractProduce", at = @At("HEAD"))
    private static void onExtractProduce(Entity entity, BlockState state, Level level, BlockPos pos,
                                         CallbackInfoReturnable<BlockState> cir) {
            checkResponsobility(state, entity.level(), pos, entity);
    }

    @Inject(method = "empty", at = @At("HEAD"))
    private static void onEmpty(Entity entity, BlockState state, LevelAccessor level, BlockPos pos,
                                CallbackInfoReturnable<BlockState> cir) {
            checkResponsobility(state, entity.level(), pos, entity);
    }

    @Inject(method = "addItem", at = @At("HEAD"))
    private static void onAddItem(Entity entity, BlockState state, LevelAccessor level, BlockPos pos,
                                  ItemStack itemStack, CallbackInfoReturnable<BlockState> cir) {
            checkResponsobility(state, entity.level(), pos, entity);
    }

    @Unique
    private static void checkResponsobility(BlockState state, Level level, BlockPos pos, Entity entity) {
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