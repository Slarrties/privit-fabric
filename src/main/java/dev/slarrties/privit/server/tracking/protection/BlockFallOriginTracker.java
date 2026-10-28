package dev.slarrties.privit.server.tracking.protection;

import dev.slarrties.privit.server.tracking.origin.TimestampedBlockOriginTracker;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.UUID;

public final class BlockFallOriginTracker extends TimestampedBlockOriginTracker {

    public BlockFallOriginTracker(ServerLevel level) { super(level); }

    @Override
    public void onServerTick() {
        if (records.isEmpty()) return;

        long expireBefore = level.getGameTime() - 10L; // getTime()
        records.entrySet().removeIf(entry -> entry.getValue().timestamp() < expireBefore);
        this.responsible.entrySet().removeIf(entry -> !records.containsKey(entry.getKey()));
    }

    @Override
    public boolean isPersistent() { return false; }
}