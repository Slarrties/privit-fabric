package dev.slarrties.privit.server.region.protection.mixin.use_fluids;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;
import dev.slarrties.privit.server.tracking.protection.*;
import dev.slarrties.privit.server.tracking.origin.TimestampedBlockOriginTracker;
import dev.slarrties.privit.server.tracking.redstone.handler.RedstoneReceiverHandler;

import net.minecraft.fluid.Fluids;
import net.minecraft.world.World;
import net.minecraft.block.Blocks;
import net.minecraft.block.IceBlock;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.state.property.Properties;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;

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
    private void onIceMelt(BlockState state, World world, BlockPos pos, CallbackInfo ci) {
        if (!(world instanceof ServerWorld serverWorld)) return;

        var trackerManager = WorldRegistry.get(serverWorld).getTrackerManager();
        IceOriginTracker iceTracker = trackerManager.getIceOriginTracker();
        FluidOriginTracker fluidTracker = trackerManager.getFluidOriginTracker();

        TimestampedBlockOriginTracker.ResponsibleTimestamp iceRecord = iceTracker.getResponsibleTimestamp(pos);
        TimestampedBlockOriginTracker.ResponsibleTimestamp heatRecord = findNearestHeatSourceRecord(pos, serverWorld, 3);

        iceTracker.remove(pos);

        TimestampedBlockOriginTracker.ResponsibleTimestamp culprit = chooseCulprit(iceRecord, heatRecord);

        if (culprit == null) return;
        if (serverWorld.getDimension().ultrawarm()) return;

        ServerPlayerEntity player = serverWorld.getServer()
                .getPlayerManager()
                .getPlayer(culprit.owner());

        if (player == null) {
            ci.cancel();
            return;
        }

        boolean allowed = RegionPermissionChecker.isAllowed(player, Rule.USE_FLUIDS, pos);

        if (!allowed) {
            ci.cancel();
            return;
        }

        fluidTracker.record(pos, player.getUuid());
    }

    @Unique
    @Nullable
    private TimestampedBlockOriginTracker.ResponsibleTimestamp chooseCulprit(
            @Nullable TimestampedBlockOriginTracker.ResponsibleTimestamp ice, @Nullable TimestampedBlockOriginTracker.ResponsibleTimestamp heat) {
        if (ice == null && heat == null) return null;
        if (ice == null) return heat;
        if (heat == null) return ice;

        return heat.timestamp() >= ice.timestamp() ? heat : ice;
    }

    @Unique
    @Nullable
    private TimestampedBlockOriginTracker.ResponsibleTimestamp findNearestHeatSourceRecord(BlockPos icePos, ServerWorld world, int radius) {
        var trackerManager = WorldRegistry.get(world).getTrackerManager();
        HeatSourceOriginTracker heatTracker = trackerManager.getHeatSourceOriginTracker();
        FluidOriginTracker fluidTracker = trackerManager.getFluidOriginTracker();
        FireOriginTracker fireTracker = trackerManager.getFireOriginTracker();
        CampfireOriginTracker campfireTracker = trackerManager.getCampfireOriginTracker();

        TimestampedBlockOriginTracker.ResponsibleTimestamp nearest = null;
        int nearestDistance = Integer.MAX_VALUE;
        long now = world.getTime();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    int dist = Math.max(Math.max(Math.abs(dx), Math.abs(dy)), Math.abs(dz));
                    if (dist == 0 || dist > radius) continue;

                    BlockPos checkPos = icePos.add(dx, dy, dz);
                    BlockState state = world.getBlockState(checkPos);
                    TimestampedBlockOriginTracker.ResponsibleTimestamp candidate = null;

                    candidate = heatTracker.getResponsibleTimestamp(checkPos);

                    if (candidate == null && world.getFluidState(checkPos).isOf(Fluids.LAVA)) {
                        TimestampedBlockOriginTracker.ResponsibleTimestamp lavaRecord = fluidTracker.getResponsibleTimestamp(checkPos);

                        if (lavaRecord != null) {
                            candidate = lavaRecord;
                        }
                    }

                    if (candidate == null && (state.isOf(Blocks.FIRE) || state.isOf(Blocks.SOUL_FIRE))) {
                        TimestampedBlockOriginTracker.ResponsibleTimestamp fireRecord = fireTracker.getResponsibleTimestamp(checkPos);
                        if (fireRecord != null) {
                            candidate = fireRecord;
                        }
                    }

                    if (candidate == null
                            && (state.isOf(Blocks.CAMPFIRE) || state.isOf(Blocks.SOUL_CAMPFIRE))
                            && state.contains(Properties.LIT)
                            && state.get(Properties.LIT)) {
                        candidate = campfireTracker.getResponsibleTimestamp(checkPos);
                    }

                    if (candidate == null && isLitLamp(state)) {
                        UUID responsible = RedstoneReceiverHandler.findResponsiblePlayer(world, checkPos);
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
        if (state.isOf(Blocks.REDSTONE_LAMP)) return state.contains(Properties.LIT) && state.get(Properties.LIT);
        if (state.isOf(Blocks.COPPER_BULB)
                || state.isOf(Blocks.EXPOSED_COPPER_BULB)
                || state.isOf(Blocks.WEATHERED_COPPER_BULB)
                || state.isOf(Blocks.OXIDIZED_COPPER_BULB)
                || state.isOf(Blocks.WAXED_COPPER_BULB)
                || state.isOf(Blocks.WAXED_EXPOSED_COPPER_BULB)
                || state.isOf(Blocks.WAXED_WEATHERED_COPPER_BULB)
                || state.isOf(Blocks.WAXED_OXIDIZED_COPPER_BULB)) {
            return state.contains(Properties.LIT) && state.get(Properties.LIT);
        }

        return false;
    }
}