package dev.slarrties.privit.common.config.sections;

import dev.slarrties.privit.common.config.annotation.ConfigValue;
import dev.slarrties.privit.common.config.annotation.ConfigSection;

@ConfigSection(
        path = "region_limits",
        comment = " Limits related to region size"
)
public class RegionLimitsSection {

    @ConfigValue(
            key = "max_area",
            comment = {
                    " Maximum region size in blocks.",
                    " If a player tries to create or resize a region beyond this value,",
                    " the operation will be denied.",
                    " 0 or a negative value disables this limit."
            }
    )
    public int maxArea = 128 * 64 * 128;

    @ConfigValue(
            key = "max_regions_per_player",
            comment = {
                    " Maximum number of regions a single player can own.",
                    " This limit is checked when a new region is created.",
                    " 0 or a negative value disables this limit."
            }
    )
    public int maxRegionsPerPlayer = 5;
}