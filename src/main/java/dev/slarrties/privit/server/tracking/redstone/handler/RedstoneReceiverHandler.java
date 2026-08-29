package dev.slarrties.privit.server.tracking.redstone.handler;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;

import net.minecraft.util.math.BlockPos;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;


// TODO: why is this layer needed? Is it an unnecessary division of logic?
public final class RedstoneReceiverHandler {

    private RedstoneReceiverHandler() {}

    public static boolean isActionAllowed(ServerWorld serverWorld, BlockPos pos, Rule rule, @Nullable BlockPos actionPos) {
        UUID responsible = findResponsiblePlayer(serverWorld, actionPos != null ? actionPos : pos);
        if (responsible == null) return true;

        return RegionPermissionChecker.isAllowed(responsible, rule, pos, serverWorld);
    }

    @Nullable
    public static UUID findResponsiblePlayer(ServerWorld world, BlockPos actionPos) {
        if (actionPos == null) return null;

        // faster ???
//        ServerPlayerEntity direct = WorldRegionManager.get(world)
//                .getTrackerManager()
//                .getRedstoneOriginTracker()
//                .getResponsible(actionPos);

//        if (direct != null) return direct;

        return WorldRegistry.get(world)
                .getTrackerManager()
                .getRedstoneTraceService()
                .getResponsiblePlayer(actionPos);
    }
}