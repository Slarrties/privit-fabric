package dev.slarrties.privit.server.tracking.protection;

import dev.slarrties.privit.server.tracking.origin.AbstractEntityOriginTracker;

import net.minecraft.server.level.ServerLevel;

public class InfluencedEntityTracker extends AbstractEntityOriginTracker {
    public InfluencedEntityTracker(ServerLevel level) { super(level); }
}