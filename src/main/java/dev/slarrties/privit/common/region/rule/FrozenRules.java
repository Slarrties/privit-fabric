package dev.slarrties.privit.common.region.rule;

import dev.slarrties.privit.common.config.PrivitConfig;
import dev.slarrties.privit.common.config.ConfigManager;

import java.util.Set;
import java.util.List;
import java.util.Locale;
import java.util.EnumSet;
import java.util.concurrent.CopyOnWriteArrayList;

public final class FrozenRules {

    private static final Set<Rule> FROZEN = EnumSet.noneOf(Rule.class);
    private static final List<RuleFreezeObserver> OBSERVERS = new CopyOnWriteArrayList<>();

    private FrozenRules() {}

    public static void init() {
        applyFromConfig(ConfigManager.get(), false);
        ConfigManager.addListener(FrozenRules::onConfigReloaded);
    }

    public static boolean isFrozen(Rule rule) {
        return FROZEN.contains(rule);
    }

    public static void addObserver(RuleFreezeObserver observer) {
        if (observer == null || OBSERVERS.contains(observer)) return;
        OBSERVERS.add(observer);
    }

    public static void removeObserver(RuleFreezeObserver observer) {
        OBSERVERS.remove(observer);
    }

    private static void onConfigReloaded(PrivitConfig config) {
        applyFromConfig(config, true);
    }

    private static void applyFromConfig(PrivitConfig config, boolean notify) {
        Set<Rule> next = parse(config.frozenRules.disabled);

        for (Rule rule : Rule.values()) {
            if (rule == Rule.MANAGE) continue;

            boolean wasFrozen = FROZEN.contains(rule);
            boolean nowFrozen = next.contains(rule);
            if (wasFrozen == nowFrozen) continue;

            if (nowFrozen) {
                FROZEN.add(rule);
            } else {
                FROZEN.remove(rule);
            }

            if (notify) {
                notifyObservers(rule, nowFrozen);
            }
        }
    }

    private static Set<Rule> parse(List<String> names) {
        Set<Rule> result = EnumSet.noneOf(Rule.class);
        if (names == null) return result;

        for (String raw : names) {
            if (raw == null || raw.isBlank()) continue;
            try {
                Rule rule = Rule.valueOf(raw.trim().toUpperCase(Locale.ROOT));
                if (rule != Rule.MANAGE) result.add(rule);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return result;
    }

    private static void notifyObservers(Rule rule, boolean nowFrozen) {
        for (RuleFreezeObserver observer : OBSERVERS) {
            observer.onFreezeChanged(rule, nowFrozen);
        }
    }
}