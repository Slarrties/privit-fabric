package dev.slarrties.privit.client.render;

import net.minecraft.util.math.BlockBox;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class RegionRenderCache {

    private static final RegionRenderCache INSTANCE = new RegionRenderCache();
    private final Map<UUID, RegionRenderEntry> entries = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> gridVisibilityOverrides = new ConcurrentHashMap<>();

    private RegionRenderCache() {}

    public static RegionRenderCache getInstance() { return INSTANCE; }

    public void updateEntry(RegionRenderEntry entry) {
        entries.put(entry.regionId(), entry);
    }

    public void updateDraft(UUID regionId, BlockBox draft) {
        entries.computeIfPresent(regionId, (id, old) -> old.withDraft(draft));
    }

    public void updateConflicts(UUID regionId, List<BlockBox> conflicts) {
        entries.computeIfPresent(regionId, (id, old) -> old.withConflicts(conflicts));
    }

    public void setGridVisible(UUID regionId, boolean visible) {
        gridVisibilityOverrides.put(regionId, visible);
        entries.computeIfPresent(regionId, (id, old) -> old.withGridVisible(visible));
    }

    public boolean isGridVisible(UUID regionId) {
        Boolean override = gridVisibilityOverrides.get(regionId);
        if (override != null) return override;

        RegionRenderEntry entry = entries.get(regionId);
        return entry != null && entry.isGridVisible();
    }

    public List<RegionRenderEntry> getActiveEntries() {
        return entries.values().stream().filter(RegionRenderEntry::shouldRender).toList();
    }

    public void cleanupInactive(long maxInactiveTimeMs) {
        long now = System.currentTimeMillis();
        entries.entrySet().removeIf(entry ->
                !entry.getValue().isGridVisible() && (now - entry.getValue().lastUpdateTime() > maxInactiveTimeMs)
        );
    }

    public void clear() {
        entries.clear();
        gridVisibilityOverrides.clear();
    }

    public int size() { return entries.size(); }

    public RegionRenderEntry getEntryIfPresent(UUID regionId) {
        return entries.get(regionId);
    }

    public void updateOrMerge(RegionRenderEntry newEntry) {
        Boolean override = gridVisibilityOverrides.get(newEntry.regionId());
        RegionRenderEntry existing = entries.get(newEntry.regionId());

        if (existing != null) {
            boolean targetVisible = override != null ? override : existing.isGridVisible();
            RegionRenderEntry merged = newEntry.withGridVisible(targetVisible);
            entries.put(newEntry.regionId(), merged);
        } else {
            boolean targetVisible = override != null ? override : newEntry.isGridVisible();
            entries.put(newEntry.regionId(), newEntry.withGridVisible(targetVisible));
        }
    }

    public void remove(UUID regionId) {
        if (regionId != null) {
            entries.remove(regionId);
            gridVisibilityOverrides.remove(regionId);
        }
    }
}