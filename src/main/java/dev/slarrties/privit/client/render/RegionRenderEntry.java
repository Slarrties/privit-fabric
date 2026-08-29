package dev.slarrties.privit.client.render;

import dev.slarrties.privit.common.region.Color;

import net.minecraft.util.math.BlockBox;

import java.util.List;
import java.util.UUID;
import java.util.Objects;

public record RegionRenderEntry(
        UUID regionId,
        Color color,
        BlockBox original,
        BlockBox draft,
        List<BlockBox> conflicts,
        boolean isGridVisible,
        long lastUpdateTime
) {

    public RegionRenderEntry {
        Objects.requireNonNull(regionId, "regionId cannot be null");
        Objects.requireNonNull(color, "color cannot be null");
        Objects.requireNonNull(conflicts, "conflicts cannot be null");
    }

    public static RegionRenderEntry create(UUID regionId, Color color, BlockBox original) {
        return new RegionRenderEntry(
                regionId,
                color,
                original,
                null,
                List.of(),
                false,
                System.currentTimeMillis()
        );
    }

    public RegionRenderEntry withDraft(BlockBox newDraft) {
        return new RegionRenderEntry(
                regionId,
                color,
                original,
                newDraft,
                conflicts,
                isGridVisible,
                System.currentTimeMillis()
        );
    }

    public RegionRenderEntry withConflicts(List<BlockBox> newConflicts) {
        return new RegionRenderEntry(
                regionId,
                color,
                original,
                draft,
                newConflicts != null ? newConflicts : List.of(),
                isGridVisible,
                System.currentTimeMillis()
        );
    }

    public RegionRenderEntry withGridVisible(boolean visible) {
        return new RegionRenderEntry(
                regionId,
                color,
                original,
                draft,
                conflicts,
                visible,
                lastUpdateTime
        );
    }

    public boolean hasOriginal() {
        return original != null;
    }

    public boolean hasDraft() {
        return draft != null;
    }

    public boolean shouldRender() {
        return isGridVisible && (hasOriginal() || hasDraft() || !conflicts.isEmpty());
    }
}