package dev.slarrties.privit.server.tracking.protection;

import dev.slarrties.privit.server.tracking.origin.AbstractEntityOriginTracker;

import net.minecraft.server.level.ServerLevel;

public final class MinecartOriginTracker extends AbstractEntityOriginTracker {
    public MinecartOriginTracker(ServerLevel level) { super(level); }
}