package dev.slarrties.privit.server.tracking.protection;

import dev.slarrties.privit.server.tracking.origin.TimestampedBlockOriginTracker;

import net.minecraft.util.math.BlockPos;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class CampfireOriginTracker extends TimestampedBlockOriginTracker {

    public CampfireOriginTracker(ServerWorld world) {
        super(world);
    }

    @Override
    public void record(BlockPos pos, UUID playerUuid) {
        super.record(pos, playerUuid);
    }

//    @Nullable
//    @Override
//    public UUID getOwner(BlockPos pos) {
//        return super.getOwner(pos);
//    }

    @Nullable
    public TimestampedBlockOriginTracker.ResponsibleTimestamp getResponsibleTimestamp(BlockPos pos) {
        return super.getResponsibleTimestamp(pos);
    }

    @Override
    public void remove(BlockPos pos) {
        super.remove(pos);
    }
}