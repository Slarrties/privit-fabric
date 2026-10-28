package dev.slarrties.privit.client.render.state;

import dev.slarrties.privit.client.render.edge.EdgeMesh;
import dev.slarrties.privit.client.render.edge.EdgeCompiler;
import dev.slarrties.privit.common.region.Color;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.List;
import java.util.UUID;
import java.util.Objects;

public record RegionRenderEntry(
        UUID regionId, Color color, BoundingBox original, BoundingBox draft, List<BoundingBox> conflicts,
        boolean isGridVisible, long lastUpdateTime, EdgeMesh cachedEdges, AABB boundingBox
) {
    public RegionRenderEntry {
        Objects.requireNonNull(regionId); Objects.requireNonNull(color); Objects.requireNonNull(conflicts);
        Objects.requireNonNull(cachedEdges); Objects.requireNonNull(boundingBox);
    }

    public static RegionRenderEntry create(UUID regionId, Color color, BoundingBox original) {
        return buildEntry(regionId, color, original, null, List.of(), false);
    }

    private static RegionRenderEntry buildEntry(UUID id, Color color, BoundingBox orig, BoundingBox drft, List<BoundingBox> conf, boolean visible) {
        RegionRenderEntry temp = new RegionRenderEntry(id, color, orig, drft, conf, visible, System.currentTimeMillis(), EdgeMesh.EMPTY, new AABB(0,0,0,0,0,0));
        EdgeMesh edges = EdgeCompiler.compile(temp);
        AABB bounds = computeBoundingBox(orig, drft, conf);
        return new RegionRenderEntry(id, color, orig, drft, conf, visible, System.currentTimeMillis(), edges, bounds);
    }

    private static AABB computeBoundingBox(BoundingBox orig, BoundingBox drft, List<BoundingBox> conflicts) {
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
        boolean hasBounds = false;

        if (orig != null) {
            minX = Math.min(minX, orig.minX()); minY = Math.min(minY, orig.minY()); minZ = Math.min(minZ, orig.minZ());
            maxX = Math.max(maxX, orig.maxX() + 1); maxY = Math.max(maxY, orig.maxY() + 1); maxZ = Math.max(maxZ, orig.maxZ() + 1);
            hasBounds = true;
        }
        if (drft != null) {
            minX = Math.min(minX, drft.minX()); minY = Math.min(minY, drft.minY()); minZ = Math.min(minZ, drft.minZ());
            maxX = Math.max(maxX, drft.maxX() + 1); maxY = Math.max(maxY, drft.maxY() + 1); maxZ = Math.max(maxZ, drft.maxZ() + 1);
            hasBounds = true;
        }
        for (BoundingBox c : conflicts) {
            minX = Math.min(minX, c.minX()); minY = Math.min(minY, c.minY()); minZ = Math.min(minZ, c.minZ());
            maxX = Math.max(maxX, c.maxX() + 1); maxY = Math.max(maxY, c.maxY() + 1); maxZ = Math.max(maxZ, c.maxZ() + 1);
            hasBounds = true;
        }
        return hasBounds ? new AABB(minX, minY, minZ, maxX, maxY, maxZ) : new AABB(0, 0, 0, 0, 0, 0);
    }

    public RegionRenderEntry withDraft(BoundingBox newDraft) { return buildEntry(regionId, color, original, newDraft, conflicts, isGridVisible); }
    public RegionRenderEntry withConflicts(List<BoundingBox> newConflicts) { return buildEntry(regionId, color, original, draft, newConflicts != null ? newConflicts : List.of(), isGridVisible); }
    public RegionRenderEntry withGridVisible(boolean visible) { return new RegionRenderEntry(regionId, color, original, draft, conflicts, visible, lastUpdateTime, cachedEdges, boundingBox); }
    public boolean hasOriginal() { return original != null; }
    public boolean hasDraft() { return draft != null; }
    public boolean shouldRender() { return isGridVisible && (hasOriginal() || hasDraft() || !conflicts.isEmpty()); }
}