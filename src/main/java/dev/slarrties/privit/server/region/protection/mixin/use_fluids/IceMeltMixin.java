package dev.slarrties.privit.server.region.protection.mixin.use_fluids;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;
import dev.slarrties.privit.server.tracking.protection.*;
import dev.slarrties.privit.server.tracking.origin.TimestampedBlockOriginTracker;
import dev.slarrties.privit.server.tracking.redstone.handler.RedstoneReceiverHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.IceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@AssociatedRule(Rule.USE_FLUIDS)
@Mixin(IceBlock.class)
public abstract class IceMeltMixin {

    @Inject(method = "melt", at = @At("HEAD"), cancellable = true)
    private void onIceMelt(BlockState state, Level level, BlockPos pos, CallbackInfo ci) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        var trackerManager = WorldRegistry.get(serverLevel).getTrackerManager();
        IceOriginTracker iceTracker = trackerManager.getIceOriginTracker();
        FluidOriginTracker fluidTracker = trackerManager.getFluidOriginTracker();
        TimestampedBlockOriginTracker.ResponsibleTimestamp iceRecord = iceTracker.getResponsibleTimestamp(pos);
        TimestampedBlockOriginTracker.ResponsibleTimestamp heatRecord = findNearestHeatSourceRecord(pos, serverLevel, 3);
        iceTracker.remove(pos);

        TimestampedBlockOriginTracker.ResponsibleTimestamp culprit = chooseCulprit(iceRecord, heatRecord);
        if (culprit == null) return;

        // TODO
//        if (serverLevel.dimensionType().ultraWarm()) return;

        ServerPlayer player = serverLevel.getServer()
                .getPlayerList()
                .getPlayer(culprit.owner());
        if (player == null) {
            ci.cancel();
            return;
        }

        boolean allowed = RegionPermissionChecker.isAllowed(player.getUUID(), Rule.USE_FLUIDS, pos, serverLevel);
        if (!allowed) {
            ci.cancel();
            return;
        }

        fluidTracker.record(pos, player.getUUID());
    }

    @Unique
    @Nullable
    private TimestampedBlockOriginTracker.ResponsibleTimestamp chooseCulprit(
            @Nullable TimestampedBlockOriginTracker.ResponsibleTimestamp ice,
            @Nullable TimestampedBlockOriginTracker.ResponsibleTimestamp heat) {
        if (ice == null && heat == null) return null;
        if (ice == null) return heat;
        if (heat == null) return ice;
        return heat.timestamp() >= ice.timestamp() ? heat : ice;
    }

    @Unique
    @Nullable
    private TimestampedBlockOriginTracker.ResponsibleTimestamp findNearestHeatSourceRecord(
            BlockPos icePos, ServerLevel serverLevel, int radius) {
        var trackerManager = WorldRegistry.get(serverLevel).getTrackerManager();
        HeatSourceOriginTracker heatTracker = trackerManager.getHeatSourceOriginTracker();
        FluidOriginTracker fluidTracker = trackerManager.getFluidOriginTracker();
        FireOriginTracker fireTracker = trackerManager.getFireOriginTracker();
        CampfireOriginTracker campfireTracker = trackerManager.getCampfireOriginTracker();
        TimestampedBlockOriginTracker.ResponsibleTimestamp nearest = null;
        int nearestDistance = Integer.MAX_VALUE;
        long now = serverLevel.getGameTime();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    int dist = Math.max(Math.max(Math.abs(dx), Math.abs(dy)), Math.abs(dz));
                    if (dist == 0 || dist > radius) continue;

                    BlockPos checkPos = icePos.offset(dx, dy, dz);
                    BlockState state = serverLevel.getBlockState(checkPos);
                    TimestampedBlockOriginTracker.ResponsibleTimestamp candidate = null;

                    candidate = heatTracker.getResponsibleTimestamp(checkPos);

                    if (candidate == null && serverLevel.getFluidState(checkPos).is(Fluids.LAVA)) {
                        TimestampedBlockOriginTracker.ResponsibleTimestamp lavaRecord = fluidTracker.getResponsibleTimestamp(checkPos);
                        if (lavaRecord != null) {
                            candidate = lavaRecord;
                        }
                    }

                    if (candidate == null && (state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE))) {
                        TimestampedBlockOriginTracker.ResponsibleTimestamp fireRecord = fireTracker.getResponsibleTimestamp(checkPos);
                        if (fireRecord != null) {
                            candidate = fireRecord;
                        }
                    }

                    if (candidate == null
                            && (state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE))
                            && state.hasProperty(BlockStateProperties.LIT)
                            && state.getValue(BlockStateProperties.LIT)) {
                        candidate = campfireTracker.getResponsibleTimestamp(checkPos);
                    }

                    if (candidate == null && isLitLamp(state)) {
                        UUID responsible = RedstoneReceiverHandler.findResponsiblePlayer(serverLevel, checkPos);
                        if (responsible != null) {
                            candidate = new TimestampedBlockOriginTracker.ResponsibleTimestamp(responsible, now);
                        }
                    }

                    if (candidate == null) continue;

                    if (dist < nearestDistance || (dist == nearestDistance && nearest != null && candidate.timestamp() > nearest.timestamp())) {
                        nearestDistance = dist;
                        nearest = candidate;
                    }
                }
            }
        }
        return nearest;
    }

    @Unique
    private static boolean isLitLamp(BlockState state) {
        if (state.is(Blocks.REDSTONE_LAMP))
            return state.hasProperty(BlockStateProperties.LIT) && state.getValue(BlockStateProperties.LIT);

        if (state.is(Blocks.COPPER_BULB)
                || state.is(Blocks.EXPOSED_COPPER_BULB)
                || state.is(Blocks.WEATHERED_COPPER_BULB)
                || state.is(Blocks.OXIDIZED_COPPER_BULB)
                || state.is(Blocks.WAXED_COPPER_BULB)
                || state.is(Blocks.WAXED_EXPOSED_COPPER_BULB)
                || state.is(Blocks.WAXED_WEATHERED_COPPER_BULB)
                || state.is(Blocks.WAXED_OXIDIZED_COPPER_BULB)) {
            return state.hasProperty(BlockStateProperties.LIT) && state.getValue(BlockStateProperties.LIT);
        }
        return false;
    }
}