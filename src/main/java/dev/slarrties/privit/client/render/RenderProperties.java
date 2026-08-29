package dev.slarrties.privit.client.render;

import dev.slarrties.privit.common.region.Color;

public record RenderProperties(
        float red,
        float green,
        float blue,
        float alpha,

        float edgeRed,
        float edgeGreen,
        float edgeBlue,
        float edgeAlpha,
        boolean isDashed,
        float edgeThickness
) {

    public static RenderProperties fromColor(Color color, float fillAlpha, boolean isDashed) {
        Integer val = color.getFormatting().getColorValue();
        int rgb = val != null ? val : 0x88CCFF;

        float r = ((rgb >> 16) & 0xFF) / 255f;
        float g = ((rgb >> 8) & 0xFF) / 255f;
        float b = (rgb & 0xFF) / 255f;

        float edgeMultiplier = 1.55f;
        float edgeR = Math.min(1.0f, r * edgeMultiplier);
        float edgeG = Math.min(1.0f, g * edgeMultiplier);
        float edgeB = Math.min(1.0f, b * edgeMultiplier);

        return new RenderProperties(
                r, g, b, fillAlpha,
                edgeR, edgeG, edgeB, 1.0f,
                isDashed,
                0.022f
        );
    }
}