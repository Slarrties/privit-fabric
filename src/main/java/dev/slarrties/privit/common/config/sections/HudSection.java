package dev.slarrties.privit.common.config.sections;

import dev.slarrties.privit.common.config.annotation.ConfigValue;
import dev.slarrties.privit.common.config.annotation.ConfigSection;

@ConfigSection(
        path = "hud",
        comment = " HUD widget settings."
)
public class HudSection {

    public enum Position {
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT;

        public static Position fromString(String value) {
            if (value == null || value.isBlank()) return TOP_RIGHT;
            try {
                return Position.valueOf(value.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                return TOP_RIGHT;
            }
        }
    }

    @ConfigValue(
            key = "region_name_position",
            comment = {
                    " Position of the region name widget on the screen.",
                    " Allowed values:",
                    "  TOP_LEFT",
                    "  TOP_RIGHT",
                    "  BOTTOM_LEFT",
                    "  BOTTOM_RIGHT"
            }
    )
    public Position regionNamePosition = Position.TOP_RIGHT;
}