package dev.slarrties.privit.server.tracking.protection;

import dev.slarrties.privit.server.tracking.origin.TimestampedBlockOriginTracker;

import net.minecraft.util.math.BlockPos;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class FireOriginTracker extends TimestampedBlockOriginTracker {

    public FireOriginTracker(ServerWorld world) {
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
    @Override
    public UUID getResponsible(ServerWorld world, BlockPos pos) {
        return super.getResponsible(world, pos);
    }

    @Override
    public void remove(BlockPos pos) {
        super.remove(pos);
    }
}