package dev.slarrties.privit.server.tracking.protection;

import dev.slarrties.privit.server.tracking.origin.TimestampedBlockOriginTracker;

import net.minecraft.util.math.BlockPos;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class IceOriginTracker extends TimestampedBlockOriginTracker {

    public IceOriginTracker(ServerWorld world) {
        super(world);
    }

    @Override
    public void record(BlockPos pos, UUID playerUuid) {
        super.record(pos, playerUuid);
    }

    @Nullable
    public TimestampedBlockOriginTracker.ResponsibleTimestamp getResponsibleTimestamp(BlockPos pos) {
        return super.getResponsibleTimestamp(pos);
    }

    @Override
    public void remove(BlockPos pos) {
        super.remove(pos);
    }
}