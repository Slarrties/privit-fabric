package dev.slarrties.privit.server.tracking.protection;

import dev.slarrties.privit.server.tracking.origin.TimestampedBlockOriginTracker;

import net.minecraft.util.math.BlockPos;
import net.minecraft.server.world.ServerWorld;

import java.util.UUID;

public final class BlockFallOriginTracker extends TimestampedBlockOriginTracker {

    public BlockFallOriginTracker(ServerWorld world) {
        super(world);
    }

    @Override
    public void record(BlockPos pos, UUID playerUuid) {
        if (pos == null || playerUuid == null) return;

        super.record(pos, playerUuid);
    }

    @Override
    public void onServerTick() {
        if (records.isEmpty()) return;

        long expireBefore = world.getTime() - 10L;
        records.entrySet().removeIf(entry -> entry.getValue().timestamp() < expireBefore);
        this.responsible.entrySet().removeIf(entry -> !records.containsKey(entry.getKey()));
    }

    @Override
    public boolean isPersistent() {
        return false;
    }
}