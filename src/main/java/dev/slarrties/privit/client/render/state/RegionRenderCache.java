package dev.slarrties.privit.client.render.state;

import dev.slarrties.privit.client.render.face.FaceMesh;
import dev.slarrties.privit.client.render.face.FaceCompiler;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class RegionRenderCache {

    private final Map<UUID, RegionRenderEntry> entries = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> gridVisibilityOverrides = new ConcurrentHashMap<>();
    private volatile List<FaceMesh> cachedMeshes = List.of();
    private volatile boolean geometryDirty = true;

    public void updateEntry(RegionRenderEntry entry) { entries.put(entry.regionId(), entry); markDirty(); }
    public void setGridVisible(UUID regionId, boolean visible) { gridVisibilityOverrides.put(regionId, visible); entries.computeIfPresent(regionId, (id, old) -> old.withGridVisible(visible)); markDirty(); }
    public boolean isGridVisible(UUID regionId) { Boolean override = gridVisibilityOverrides.get(regionId); return override != null ? override : entries.containsKey(regionId) && entries.get(regionId).isGridVisible(); }
    public List<RegionRenderEntry> getActiveEntries() { return entries.values().stream().filter(RegionRenderEntry::shouldRender).toList(); }

    public List<FaceMesh> getCachedMeshes() {
        if (geometryDirty) {
            synchronized (this) {
                if (geometryDirty) { cachedMeshes = FaceCompiler.compile(getActiveEntries()); geometryDirty = false; }
            }
        }
        return cachedMeshes;
    }

    private void markDirty() { geometryDirty = true; }

    public void cleanupInactive(long maxInactiveTimeMs) {
        long now = System.currentTimeMillis();
        boolean removed = entries.entrySet().removeIf(e -> !e.getValue().isGridVisible() && (now - e.getValue().lastUpdateTime() > maxInactiveTimeMs));
        gridVisibilityOverrides.keySet().removeIf(id -> !entries.containsKey(id));
        if (removed) markDirty();
    }

    public void clear() { entries.clear(); gridVisibilityOverrides.clear(); markDirty(); }

    public void updateOrMerge(RegionRenderEntry newEntry) {
        Boolean override = gridVisibilityOverrides.get(newEntry.regionId());
        boolean targetVisible = override != null ? override : newEntry.isGridVisible();
        entries.put(newEntry.regionId(), newEntry.withGridVisible(targetVisible));
        markDirty();
    }

    public void remove(UUID regionId) {
        if (regionId != null) { entries.remove(regionId); gridVisibilityOverrides.remove(regionId); markDirty(); }
    }
}