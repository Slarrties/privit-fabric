package dev.slarrties.privit.client.render.face;

import dev.slarrties.privit.client.render.state.RenderPriority;
import dev.slarrties.privit.client.render.config.RenderConfig;
import dev.slarrties.privit.common.region.Color;

public record FaceTint(float red, float green, float blue, float alpha) {

    public static FaceTint compute(Color color, RenderPriority type, RenderConfig config) {
        float baseAlpha = getBaseAlpha(type);
        float a = Math.min(1.0f, baseAlpha * config.faceAlphaMultiplier());

        int rgb = color.getColorValue();
        float r = ((rgb >> 16) & 0xFF) / 255f;
        float g = ((rgb >> 8) & 0xFF) / 255f;
        float b = (rgb & 0xFF) / 255f;
        float targetBrightness = config.faceTargetBrightness();
        if (targetBrightness > 0.0f && targetBrightness < 1.0f) {
            float[] hsb = Color.rgbToHsb(r, g, b);
            float h = hsb[0];
            float s = hsb[1];
            float brightness = hsb[2];

            if (s > 0.15f) {
                brightness = Math.min(brightness, targetBrightness);
                s = 1.0f;
            }

            float[] finalRgb = Color.hsbToRgb(h, s, brightness);
            r = finalRgb[0];
            g = finalRgb[1];
            b = finalRgb[2];
        }

        return new FaceTint(r, g, b, a);
    }

    private static float getBaseAlpha(RenderPriority type) {
        return switch (type) {
            case ORIGINAL -> RenderConfig.ALPHA_ORIGINAL;
            case DRAFT -> RenderConfig.ALPHA_DRAFT;
            case CONFLICT -> RenderConfig.ALPHA_CONFLICT;
        };
    }
}