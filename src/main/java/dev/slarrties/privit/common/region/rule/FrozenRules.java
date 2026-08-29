package dev.slarrties.privit.common.region.rule;

import dev.slarrties.privit.common.config.ConfigManager;
import dev.slarrties.privit.common.config.sections.FrozenRulesSection;

import java.util.Set;
import java.util.List;
import java.util.HashSet;
import java.util.ArrayList;

public final class FrozenRules {

    private static final List<RuleFreezeObserver> observers = new ArrayList<>();

    private FrozenRules() {}

    public static void addObserver(RuleFreezeObserver observer) {
        observers.add(observer);
    }

    public static void removeObserver(RuleFreezeObserver observer) {
        observers.remove(observer);
    }

    public static void load() {}

    public static boolean isFrozen(Rule rule) {
        if (rule == Rule.MANAGE) {
            return false;
        }

        FrozenRulesSection section = ConfigManager.get().frozenRules;
        return section.disabled.stream()
                .anyMatch(name -> name.equalsIgnoreCase(rule.name()));
    }

    public static Set<Rule> getActiveRules() {
        Set<Rule> active = new HashSet<>();
        for (Rule rule : Rule.values()) {
            if (!isFrozen(rule)) {
                active.add(rule);
            }
        }
        return active;
    }

    public static void setFrozen(Rule rule, boolean frozen) {
        if (rule == Rule.MANAGE && frozen) {
            System.err.println("[Privit] Attempt to freeze MANAGE rule ignored — operation forbidden");
            return;
        }

        FrozenRulesSection section = ConfigManager.get().frozenRules;
        String ruleName = rule.name();

        if (frozen) {
            boolean alreadyExists = section.disabled.stream().anyMatch(name -> name.equalsIgnoreCase(ruleName));

            if (!alreadyExists) {
                section.disabled.add(ruleName);
                ConfigManager.save();
            }
        } else {
            section.disabled.removeIf(name -> name.equalsIgnoreCase(ruleName));
            ConfigManager.save();
        }

        notifyObservers(rule, frozen);
    }

    private static void notifyObservers(Rule rule, boolean frozen) {
        RuleFreezeObserver[] snapshot;

        synchronized (observers) {
            snapshot = observers.toArray(new RuleFreezeObserver[0]);
        }

        for (RuleFreezeObserver observer : snapshot) {
            try {
                observer.onFreezeChanged(rule, frozen);
            } catch (Exception e) {
                System.err.println("[Privit] Error notifying RuleFreezeObserver for rule " + rule + ": " + e);
                e.printStackTrace();
            }
        }
    }

    public static void reload() {
        ConfigManager.reload();

        for (Rule rule : Rule.values()) {
            notifyObservers(rule, isFrozen(rule));
        }
    }
}