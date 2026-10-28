package dev.slarrties.privit.server.tracking.protection;

import dev.slarrties.privit.server.tracking.origin.AbstractBlockOriginTracker;

import net.minecraft.server.level.ServerLevel;

public final class RedstoneOriginTracker extends AbstractBlockOriginTracker {
    public RedstoneOriginTracker(ServerLevel level) { super(level); }
}