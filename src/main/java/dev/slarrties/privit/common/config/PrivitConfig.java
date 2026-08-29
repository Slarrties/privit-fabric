package dev.slarrties.privit.common.config;

import dev.slarrties.privit.common.config.sections.HudSection;
import dev.slarrties.privit.common.config.sections.DefaultRulesSection;
import dev.slarrties.privit.common.config.sections.FrozenRulesSection;
import dev.slarrties.privit.common.config.sections.RegionLimitsSection;

import java.util.List;

public class PrivitConfig {

    public final HudSection hud = new HudSection();
    public final FrozenRulesSection frozenRules = new FrozenRulesSection();
    public final DefaultRulesSection defaultRules = new DefaultRulesSection();
    public final RegionLimitsSection regionLimits = new RegionLimitsSection();

    public List<Object> sections() {
        return List.of(hud, frozenRules, defaultRules, regionLimits);
    }
}