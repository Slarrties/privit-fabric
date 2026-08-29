package dev.slarrties.privit.common.config.sections;

import dev.slarrties.privit.common.config.annotation.ConfigValue;
import dev.slarrties.privit.common.config.annotation.ConfigSection;

import java.util.List;
import java.util.ArrayList;

@ConfigSection(
        path = "frozen_rules",
        comment = {
                " Frozen (globally disabled) rules",
                " Frozen rules are not evaluated and are hidden from all rule lists.",
                " If a rule is later unfrozen, its previous state is restored in every region",
                " to the value it had at the moment it was frozen."
        }
)
public class FrozenRulesSection {

    @ConfigValue(
            key = "disabled",
            comment = {
                    " To freeze a rule, add its name to this list.",
                    " You can copy rule names from the default_rules section.",
                    " Each name must be quoted, and entries must be separated by commas.",
                    " Example:",
                    "  disabled = [\"PVP\", \"BUILD\", \"CAUSE_EXPLOSION\"]"
            }
    )
    public List<String> disabled = new ArrayList<>();
}






