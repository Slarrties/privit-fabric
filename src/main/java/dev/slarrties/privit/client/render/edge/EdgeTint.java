package dev.slarrties.privit.client.render.edge;

import dev.slarrties.privit.client.render.config.RegionRenderConfig;
import dev.slarrties.privit.common.region.Color;

public record EdgeTint(float red, float green, float blue, float alpha) {

    public static EdgeTint compute(Color color, RegionRenderConfig config) {
        int rgb = color.getColorValue();
        float r = ((rgb >> 16) & 0xFF) / 255f;
        float g = ((rgb >> 8) & 0xFF) / 255f;
        float b = (rgb & 0xFF) / 255f;

        float[] hsb = Color.rgbToHsb(r, g, b);
        float targetBrightness = Math.min(1.0f, hsb[2] * config.edgeBrightnessMultiplier());
        float[] edgeRgb = Color.hsbToRgb(hsb[0], hsb[1], targetBrightness);
        float a = Math.min(1.0f, config.edgeAlphaMultiplier());

        return new EdgeTint(edgeRgb[0], edgeRgb[1], edgeRgb[2], a);
    }
}