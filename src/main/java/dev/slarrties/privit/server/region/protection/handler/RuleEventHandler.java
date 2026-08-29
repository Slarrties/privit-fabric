package dev.slarrties.privit.server.region.protection.handler;

import dev.slarrties.privit.common.region.rule.Rule;

public interface RuleEventHandler {
    Rule getRule();
    void register();
}