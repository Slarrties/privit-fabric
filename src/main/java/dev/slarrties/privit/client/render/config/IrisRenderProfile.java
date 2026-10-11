package dev.slarrties.privit.client.render.config;

public final class IrisRenderProfile extends RegionRenderConfig {
    @Override public float edgeQuadHalfThickness() { return 0.015f; }
    @Override public float faceTargetBrightness() { return 0.6f; }
    @Override public float edgeAlphaMultiplier() { return 0.8f; }
}