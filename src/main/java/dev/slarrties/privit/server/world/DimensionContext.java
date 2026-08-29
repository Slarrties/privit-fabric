package dev.slarrties.privit.server.world;

import dev.slarrties.privit.PrivitMod;
import dev.slarrties.privit.server.region.RegionManager;
import dev.slarrties.privit.server.region.gui.RegionGuiSessions;
import dev.slarrties.privit.server.region.grid.RegionGridSubscriptions;
import dev.slarrties.privit.server.tracking.protection.TrackerManager;

import net.minecraft.server.world.ServerWorld;

public final class DimensionContext {

    private final ServerWorld world;
    private final RegionManager regionManager;
    private final TrackerManager trackerManager;
    private final RegionGuiSessions regionGuiSessions;
    private final RegionGridSubscriptions gridSubscriptions;

    public DimensionContext(ServerWorld world) {
        this.world = world;
        this.regionManager = new RegionManager(world);
        this.trackerManager = new TrackerManager(world);
        this.regionGuiSessions = new RegionGuiSessions(world, regionManager);
        this.gridSubscriptions = new RegionGridSubscriptions();
    }

    public void onWorldLoad() {}

    public void onServerTick() {
        regionManager.onServerTick();
        trackerManager.onServerTick();
    }

    public void onWorldUnload() {
        regionManager.onWorldUnload();
        trackerManager.onWorldUnload();
        gridSubscriptions.clear();
    }

    public RegionManager getRegionManager() {
        return regionManager;
    }

    public TrackerManager getTrackerManager() {
        return trackerManager;
    }

    public RegionGuiSessions getRegionGuiSessions() {
        return regionGuiSessions;
    }

    public RegionGridSubscriptions getGridSubscriptions() {
        return gridSubscriptions;
    }
}