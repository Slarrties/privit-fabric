package dev.slarrties.privit.client.render.state;

import dev.slarrties.privit.client.render.edge.EdgeMesh;
import dev.slarrties.privit.client.render.edge.EdgeCompiler;
import dev.slarrties.privit.common.region.Color;

import net.minecraft.util.math.Box;
import net.minecraft.util.math.BlockBox;

import java.util.List;
import java.util.UUID;
import java.util.Objects;

public record RegionRenderEntry(
        UUID regionId, Color color, BlockBox original, BlockBox draft, List<BlockBox> conflicts,
        boolean isGridVisible, long lastUpdateTime, EdgeMesh cachedEdges, Box boundingBox
) {

    public RegionRenderEntry {
        Objects.requireNonNull(regionId); Objects.requireNonNull(color); Objects.requireNonNull(conflicts);
        Objects.requireNonNull(cachedEdges); Objects.requireNonNull(boundingBox);
    }

    public static RegionRenderEntry create(UUID regionId, Color color, BlockBox original) {
        return buildEntry(regionId, color, original, null, List.of(), false);
    }

    private static RegionRenderEntry buildEntry(UUID id, Color color, BlockBox orig, BlockBox drft, List<BlockBox> conf, boolean visible) {
        RegionRenderEntry temp = new RegionRenderEntry(id, color, orig, drft, conf, visible, System.currentTimeMillis(), EdgeMesh.EMPTY, new Box(0,0,0,0,0,0));
        EdgeMesh edges = EdgeCompiler.compile(temp);
        Box bounds = computeBoundingBox(orig, drft, conf);
        return new RegionRenderEntry(id, color, orig, drft, conf, visible, System.currentTimeMillis(), edges, bounds);
    }

    private static Box computeBoundingBox(BlockBox orig, BlockBox drft, List<BlockBox> conflicts) {
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
        boolean hasBounds = false;

        if (orig != null) {
            minX = Math.min(minX, orig.getMinX());
            minY = Math.min(minY, orig.getMinY());
            minZ = Math.min(minZ, orig.getMinZ());
            maxX = Math.max(maxX, orig.getMaxX() + 1);
            maxY = Math.max(maxY, orig.getMaxY() + 1);
            maxZ = Math.max(maxZ, orig.getMaxZ() + 1);
            hasBounds = true;
        }

        if (drft != null) {
            minX = Math.min(minX, drft.getMinX());
            minY = Math.min(minY, drft.getMinY());
            minZ = Math.min(minZ, drft.getMinZ());
            maxX = Math.max(maxX, drft.getMaxX() + 1);
            maxY = Math.max(maxY, drft.getMaxY() + 1);
            maxZ = Math.max(maxZ, drft.getMaxZ() + 1);
            hasBounds = true;
        }

        for (BlockBox c : conflicts) {
            minX = Math.min(minX, c.getMinX());
            minY = Math.min(minY, c.getMinY());
            minZ = Math.min(minZ, c.getMinZ());
            maxX = Math.max(maxX, c.getMaxX() + 1);
            maxY = Math.max(maxY, c.getMaxY() + 1);
            maxZ = Math.max(maxZ, c.getMaxZ() + 1);
            hasBounds = true;
        }

        return hasBounds ? new Box(minX, minY, minZ, maxX, maxY, maxZ) : new Box(0, 0, 0, 0, 0, 0);
    }

    public RegionRenderEntry withDraft(BlockBox newDraft) { return buildEntry(regionId, color, original, newDraft, conflicts, isGridVisible); }
    public RegionRenderEntry withConflicts(List<BlockBox> newConflicts) { return buildEntry(regionId, color, original, draft, newConflicts != null ? newConflicts : List.of(), isGridVisible); }
    public RegionRenderEntry withGridVisible(boolean visible) { return new RegionRenderEntry(regionId, color, original, draft, conflicts, visible, lastUpdateTime, cachedEdges, boundingBox); }
    public boolean hasOriginal() { return original != null; }
    public boolean hasDraft() { return draft != null; }
    public boolean shouldRender() { return isGridVisible && (hasOriginal() || hasDraft() || !conflicts.isEmpty()); }
}