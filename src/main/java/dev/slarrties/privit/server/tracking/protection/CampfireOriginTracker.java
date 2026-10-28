package dev.slarrties.privit.server.tracking.protection;

import dev.slarrties.privit.server.tracking.origin.TimestampedBlockOriginTracker;

import net.minecraft.server.level.ServerLevel;

public final class CampfireOriginTracker extends TimestampedBlockOriginTracker {
    public CampfireOriginTracker(ServerLevel level) { super(level); }
}