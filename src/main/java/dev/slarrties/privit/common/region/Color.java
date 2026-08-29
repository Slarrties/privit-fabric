package dev.slarrties.privit.common.region;

import net.minecraft.util.Formatting;

public enum Color {
    BLACK("§0", Formatting.BLACK),
    DARK_BLUE("§1", Formatting.DARK_BLUE),
    BLUE("§9", Formatting.BLUE),
    AQUA("§b", Formatting.AQUA),
    DARK_AQUA("§3", Formatting.DARK_AQUA),
    DARK_GREEN("§2", Formatting.DARK_GREEN),
    GREEN("§a", Formatting.GREEN),
    YELLOW("§e", Formatting.YELLOW),
    GOLD("§6", Formatting.GOLD),
    DARK_RED("§4", Formatting.DARK_RED),
    RED("§c", Formatting.RED),
    LIGHT_PURPLE("§d", Formatting.LIGHT_PURPLE),
    DARK_PURPLE("§5", Formatting.DARK_PURPLE),
    GRAY("§7", Formatting.GRAY),
    DARK_GRAY("§8", Formatting.DARK_GRAY),
    WHITE("§f", Formatting.WHITE);

    private final String code;
    private final Formatting formatting;

    Color(String code, Formatting formatting) {
        this.code = code;
        this.formatting = formatting;
    }

    public String getCode() { return code; }

    public Formatting getFormatting() { return formatting; }

    public static Color getDefault() { return WHITE; }

    public static Color fromCode(String code) {
        for (Color color : values()) {
            if (color.code.equals(code)) {
                return color;
            }
        }
        return getDefault();
    }
}
