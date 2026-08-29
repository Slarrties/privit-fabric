package dev.slarrties.privit.common.config.sections;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.config.annotation.ConfigValue;
import dev.slarrties.privit.common.config.annotation.ConfigSection;

import java.util.Map;
import java.util.LinkedHashMap;

@ConfigSection(
        path = "default_rules",
        comment = {
                " Default rule states applied when a new group is created.",
                " You can adjust the default state as you like",
                " to minimize the setting each time you create a new group or region"
        }
)
public class DefaultRulesSection {

    @ConfigValue(
            key = "rules",
            comment = {
                    " Default enabled/disabled state for each rule.",
                    " true  — the rule is enabled by default",
                    " false — the rule is disabled by default",
            }
    )
    public Map<String, Boolean> rules = createDefaultMap();

    private static Map<String, Boolean> createDefaultMap() {
        Map<String, Boolean> map = new LinkedHashMap<>();
        for (Rule rule : Rule.values()) {
            if (rule == Rule.MANAGE) continue;
            map.put(rule.name(), false);
        }
        return map;
    }
}