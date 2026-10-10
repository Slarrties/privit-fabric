package dev.slarrties.privit.client.render.config;

public class RegionRenderConfig {
    public static final float ALPHA_ORIGINAL = 0.40f;
    public static final float ALPHA_DRAFT    = 0.65f;
    public static final float ALPHA_CONFLICT = 0.80f;
    public static final double MAX_RENDER_DISTANCE_SQ = (32.0 * 16) * (32.0 * 16);

    public float inset() { return 0.015f; }
    public float textureScale() { return 1.0f; }
    public float dashLength() { return 0.1f; }
    public float dashGap() { return 0.1f; }
    public float edgeAlphaMultiplier() { return 1.0f; }
    public float edgeBrightnessMultiplier() { return 1.3f; }
    public float edgeQuadHalfThickness() { return 0.0f; }
    public float faceAlphaMultiplier() { return 1.0f; }
    public float faceTargetBrightness() { return 0.0f; }
}