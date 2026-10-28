package dev.slarrties.privit.server.tracking.protection;

import dev.slarrties.privit.server.tracking.origin.AbstractEntityOriginTracker;

import net.minecraft.server.level.ServerLevel;

public final class MinecartFuelTracker extends AbstractEntityOriginTracker {
    public MinecartFuelTracker(ServerLevel level) { super(level); }
}