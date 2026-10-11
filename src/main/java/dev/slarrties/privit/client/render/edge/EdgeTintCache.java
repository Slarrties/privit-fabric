package dev.slarrties.privit.client.render.edge;

import dev.slarrties.privit.client.render.config.RegionRenderConfig;
import dev.slarrties.privit.common.region.Color;

import java.util.Map;
import java.util.HashMap;

public final class EdgeTintCache {
    private final Map<Integer, EdgeTint> cache = new HashMap<>();

    public EdgeTint get(RegionRenderConfig config, Color color) {
        return cache.computeIfAbsent(color.getColorValue(), k -> EdgeTint.compute(color, config));
    }

    public void clear() { cache.clear(); }
}