package dev.slarrties.privit.client.render.face;

import dev.slarrties.privit.client.render.state.RenderPriority;
import dev.slarrties.privit.client.render.config.RenderConfig;
import dev.slarrties.privit.common.region.Color;

import java.util.Map;
import java.util.HashMap;

public final class FaceTintCache {

    private record Key(int color, RenderPriority type) {}

    private final Map<Key, FaceTint> cache = new HashMap<>();

    public FaceTint get(RenderConfig config, Color color, RenderPriority type) {
        return cache.computeIfAbsent(new Key(color.getColorValue(), type), k -> FaceTint.compute(color, type, config));
    }

    public void clear() { cache.clear(); }
}