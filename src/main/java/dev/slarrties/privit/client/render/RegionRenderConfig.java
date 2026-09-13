package dev.slarrties.privit.client.render;

import net.fabricmc.loader.api.FabricLoader;

public final class RegionRenderConfig {

    public static final float INSET = 0.015f;

    public static final float ALPHA_ORIGINAL = 0.35f;
    public static final float ALPHA_DRAFT    = 0.65f;
    public static final float ALPHA_CONFLICT = 0.70f;
    public static final float CONFLICT_ALPHA_MULTIPLIER = 1.35f;

    public static final float EDGE_THICKNESS = 2.8f;
    public static final float EDGE_BRIGHTNESS_MULTIPLIER = 1.55f;
    public static final float EDGE_DASH_LENGTH_BLOCKS = 0.1f;
    public static final float EDGE_DASH_GAP_BLOCKS = 0.1f;
    public static final double MAX_RENDER_DISTANCE_SQ = (32 * 16) * (32 * 16); // 32 chunks

    public static boolean isDashed(RenderType type) {
        return type == RenderType.DRAFT || type == RenderType.CONFLICT;
    }

    public static float getEdgeThickness(RenderType type) {
        return EDGE_THICKNESS;
    }

    // TODO: crutch. It is necessary to determine whether the shaders are enabled.
    public static boolean useShaderCompat() {
        return FabricLoader.getInstance().isModLoaded("iris");
    }

    private RegionRenderConfig() {}
}