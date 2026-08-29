package dev.slarrties.privit.client.render;

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
    public static final double MAX_RENDER_DISTANCE_SQ = (48 * 16) * (48 * 16); // 48 chunks

    public static boolean isDashed(RenderType type) {
        return type == RenderType.DRAFT || type == RenderType.CONFLICT;
    }

    public static float getEdgeThickness(RenderType type) {
        return EDGE_THICKNESS;
    }

    private RegionRenderConfig() {}
}