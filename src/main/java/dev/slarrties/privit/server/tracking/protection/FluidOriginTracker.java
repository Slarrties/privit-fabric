package dev.slarrties.privit.server.tracking.protection;

import dev.slarrties.privit.server.tracking.origin.TimestampedBlockOriginTracker;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class FluidOriginTracker extends TimestampedBlockOriginTracker {

    public FluidOriginTracker(ServerWorld world) {
        super(world);
    }

    @Override
    public void record(BlockPos pos, UUID playerUuid) {
        super.record(pos, playerUuid);
    }

    @Override
    public void propagate(BlockPos from, BlockPos to) {
        if (from == null || to == null || world.isClient || from.equals(to)) return;

        OwnershipRecord fromRecord = records.get(from.asLong());

        if (fromRecord == null) {
            for (Direction dir : Direction.values()) {
                BlockPos neighbor = from.offset(dir);
                fromRecord = records.get(neighbor.asLong());
                if (fromRecord != null) break;
            }
        }

        if (fromRecord != null) {
            records.put(to.asLong(), fromRecord);
            this.responsible.put(to.asLong(), fromRecord.owner());
        }
    }

    @Nullable
    public UUID getOwner(BlockPos pos) {
        return super.getOwner(pos);
    }

    @Nullable
    public OwnershipRecord getRecord(BlockPos pos) {
        return super.getRecord(pos);
    }

    @Nullable
    public UUID getResponsible(ServerWorld world, BlockPos pos) {
        return super.getResponsible(world, pos);
    }

    @Override
    public void remove(BlockPos pos) {
        super.remove(pos);
    }
}