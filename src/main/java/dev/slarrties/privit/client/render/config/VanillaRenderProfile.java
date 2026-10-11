package dev.slarrties.privit.client.render.config;

public final class VanillaRenderProfile extends RegionRenderConfig {
    @Override public float edgeQuadHalfThickness() { return 0.012f; }
    @Override public float faceAlphaMultiplier() { return 0.75f; }
}