package dev.slarrties.privit.common.region.rule;

public interface RuleFreezeObserver {
    void onFreezeChanged(Rule rule, boolean isFrozen);
}
