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

    public int getArgb(float alpha) {
        int a = Math.max(0, Math.min(255, (int) (alpha * 255f)));
        return (a << 24) | (rgb & 0xFFFFFF);
    }

    public static Color getDefault() { return WHITE; }

    public static Color fromCode(String code) {
        if (code == null) return getDefault();
        for (Color color : values()) {
            if (color.code.equalsIgnoreCase(code)) return color;
        }
        return getDefault();
    }
}