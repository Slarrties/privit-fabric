package dev.slarrties.privit.server.tracking.protection;

import net.minecraft.server.level.ServerLevel;
import dev.slarrties.privit.server.tracking.origin.AbstractEntityOriginTracker;

public final class ExplosionOriginTracker extends AbstractEntityOriginTracker {
    public ExplosionOriginTracker(ServerLevel level) { super(level); }
}