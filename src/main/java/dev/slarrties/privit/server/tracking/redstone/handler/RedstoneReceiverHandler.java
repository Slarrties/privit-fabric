package dev.slarrties.privit.server.tracking.redstone.handler;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class RedstoneReceiverHandler {
    private RedstoneReceiverHandler() {}

    public static boolean isActionAllowed(ServerLevel serverLevel, BlockPos pos, Rule rule, @Nullable BlockPos actionPos) {
        UUID responsible = findResponsiblePlayer(serverLevel, actionPos != null ? actionPos : pos);
        if (responsible == null) return true;
        return RegionPermissionChecker.isAllowed(responsible, rule, pos, serverLevel);
    }

    @Nullable
    public static UUID findResponsiblePlayer(ServerLevel level, BlockPos actionPos) {
        if (actionPos == null) return null;
        return WorldRegistry.get(level)
                .getTrackerManager()
                .getRedstoneTraceService()
                .getResponsiblePlayer(actionPos);
    }
}