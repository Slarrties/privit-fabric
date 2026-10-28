package dev.slarrties.privit.server.tracking.protection;

import dev.slarrties.privit.server.tracking.origin.TimestampedBlockOriginTracker;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;

public final class FluidOriginTracker extends TimestampedBlockOriginTracker {

    public FluidOriginTracker(ServerLevel level) { super(level); }

    @Override
    public void propagate(BlockPos from, BlockPos to) {
        if (from == null || to == null || level.isClientSide() || from.equals(to)) return;

        ResponsibleTimestamp fromRecord = records.get(from.asLong());

        if (fromRecord == null) {
            for (Direction dir : Direction.values()) {
                BlockPos neighbor = from.offset(dir.getUnitVec3i());
                fromRecord = records.get(neighbor.asLong());
                if (fromRecord != null) break;
            }
        }

        if (fromRecord != null) {
            records.put(to.asLong(), fromRecord);
            this.responsible.put(to.asLong(), fromRecord.owner());
        }
    }
}