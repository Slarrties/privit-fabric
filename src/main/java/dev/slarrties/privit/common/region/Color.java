package dev.slarrties.privit.common.region;

import net.minecraft.util.Formatting;

public enum Color {
    BLACK        ("§0", Formatting.BLACK,        0x303030),
    DARK_BLUE    ("§1", Formatting.DARK_BLUE,    0x2E42FF),
    BLUE         ("§9", Formatting.BLUE,         0x5E9AFC),
    AQUA         ("§b", Formatting.AQUA,         0x55FFFF),
    DARK_AQUA    ("§3", Formatting.DARK_AQUA,    0x00AAAA),
    DARK_GREEN   ("§2", Formatting.DARK_GREEN,   0x00AA00),
    GREEN        ("§a", Formatting.GREEN,        0x55FF55),
    YELLOW       ("§e", Formatting.YELLOW,       0xF7D300),
    GOLD         ("§6", Formatting.GOLD,         0xEF8200),
    DARK_RED     ("§4", Formatting.DARK_RED,     0xCC1212),
    RED          ("§c", Formatting.RED,          0xE83A3A),
    LIGHT_PURPLE ("§d", Formatting.LIGHT_PURPLE, 0xFF55FF),
    DARK_PURPLE  ("§5", Formatting.DARK_PURPLE,  0xAA00AA),
    DARK_GRAY    ("§8", Formatting.DARK_GRAY,    0x555555),
    GRAY         ("§7", Formatting.GRAY,         0xAAAAAA),
    WHITE        ("§f", Formatting.WHITE,        0xFFFFFF);

    private final String code;
    private final Formatting formatting;
    private final int rgb;

    Color(String code, Formatting formatting, int rgb) {
        this.code = code;
        this.formatting = formatting;
        this.rgb = rgb;
    }

    public String getCode() { return code; }

    public Formatting getFormatting() { return formatting; }

    public int getColorValue() { return rgb; }

    public static Color getDefault() { return WHITE; }

    public static Color fromCode(String code) {
        if (code == null) return getDefault();
        for (Color color : values()) {
            if (color.code.equalsIgnoreCase(code)) return color;
        }
        return getDefault();
    }

    public int getArgb(float alpha) {
        int a = Math.max(0, Math.min(255, (int) (alpha * 255f)));
        return (a << 24) | (rgb & 0xFFFFFF);
    }

    public static float[] rgbToHsb(float r, float g, float b) {
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float delta = max - min;
        float h = 0;

        if (delta != 0) {
            if (max == r) h = ((g - b) / delta) % 6;
            else if (max == g) h = ((b - r) / delta) + 2;
            else h = ((r - g) / delta) + 4;
            h /= 6;
            if (h < 0) h += 1;
        }
        float s = (max == 0) ? 0 : delta / max;

        return new float[]{h, s, max};
    }

    public static float[] hsbToRgb(float h, float s, float v) {
        float c = v * s;
        float x = c * (1 - Math.abs((h * 6) % 2 - 1));
        float m = v - c;
        float r, g, b;
        int sector = (int) (h * 6);

        switch (sector) {
            case 0: r = c; g = x; b = 0; break;
            case 1: r = x; g = c; b = 0; break;
            case 2: r = 0; g = c; b = x; break;
            case 3: r = 0; g = x; b = c; break;
            case 4: r = x; g = 0; b = c; break;
            default: r = c; g = 0; b = x; break;
        }

        return new float[]{r + m, g + m, b + m};
    }
}