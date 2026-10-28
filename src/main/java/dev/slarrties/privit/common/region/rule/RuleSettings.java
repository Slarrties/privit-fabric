package dev.slarrties.privit.common.region.rule;

import dev.slarrties.privit.common.config.ConfigManager;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

import java.util.*;

public class RuleSettings implements RuleFreezeObserver, AutoCloseable {

    private final String groupName;
    private Map<Rule, Boolean> activeRules = new EnumMap<>(Rule.class);
    private Map<Rule, Boolean> frozenStates = new EnumMap<>(Rule.class);
    private volatile boolean registered = false;
    private volatile boolean closed = false;

    public RuleSettings(String groupName) {
        this.groupName = groupName;
        register();
        initializeDefaults();
        syncWithCurrentFreezeState();
    }

    public RuleSettings(RuleSettings original) {
        this.groupName = original.groupName;
        this.activeRules = new EnumMap<>(original.activeRules);
        this.frozenStates = new EnumMap<>(original.frozenStates);
        register();
    }

    private RuleSettings(String groupName, boolean hydrateDefaults) {
        this.groupName = groupName;
        register();
        if (hydrateDefaults) {
            initializeDefaults();
            syncWithCurrentFreezeState();
        }
    }

    private void register() {
        if (registered || closed) return;
        FrozenRules.addObserver(this);
        registered = true;
    }

    private void initializeDefaults() {
        for (Rule rule : Rule.values()) {
            if (!isRuleAllowed(rule)) continue;

            boolean def;
            if ("owner".equals(groupName)) {
                def = true;
            } else {
                def = ConfigManager.get().defaultRules.rules
                        .getOrDefault(rule.name(), false);
            }
            activeRules.put(rule, def);
        }
    }

    private boolean isRuleAllowed(Rule rule) {
        return rule != Rule.MANAGE || "owner".equals(groupName);
    }

    public boolean isEnabled(Rule rule) {
        if (!isRuleAllowed(rule)) return false;
        if (FrozenRules.isFrozen(rule)) return false;

        return activeRules.getOrDefault(rule, false);
    }

    public void setRuleState(Rule rule, boolean enabled) {
        if (!isRuleAllowed(rule)) return;

        if (FrozenRules.isFrozen(rule)) {
            frozenStates.put(rule, enabled);
        } else {
            activeRules.put(rule, enabled);
        }
    }

    public List<Rule> getActiveRules() {
        return new ArrayList<>(activeRules.keySet());
    }

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        CompoundTag active = new CompoundTag();
        CompoundTag frozen = new CompoundTag();
        activeRules.forEach((r, v) -> active.putBoolean(r.name(), v));
        frozenStates.forEach((r, v) -> frozen.putBoolean(r.name(), v));
        nbt.put("active", active);
        nbt.put("frozen", frozen);
        return nbt;
    }

    public static RuleSettings fromNbt(CompoundTag nbt, String groupName) {
        RuleSettings settings = new RuleSettings(groupName);

        CompoundTag activeTag = nbt.getCompoundOrEmpty("active");
        for (String key : activeTag.keySet()) {
            try {
                Rule rule = Rule.valueOf(key);
                settings.activeRules.put(rule, activeTag.getBooleanOr(key, false));
            } catch (IllegalArgumentException ignored) {
                //
            }
        }

        CompoundTag frozenTag = nbt.getCompoundOrEmpty("frozen");
        for (String key : frozenTag.keySet()) {
            try {
                Rule rule = Rule.valueOf(key);
                settings.frozenStates.put(rule, frozenTag.getBooleanOr(key, false));
            } catch (IllegalArgumentException ignored) {}
        }

        settings.syncWithCurrentFreezeState();
        return settings;
    }

    public void writeToBuf(FriendlyByteBuf buf) {
        buf.writeVarInt(activeRules.size());
        for (Map.Entry<Rule, Boolean> entry : activeRules.entrySet()) {
            buf.writeEnum(entry.getKey());
            buf.writeBoolean(entry.getValue());
        }
        buf.writeVarInt(frozenStates.size());
        for (Map.Entry<Rule, Boolean> entry : frozenStates.entrySet()) {
            buf.writeEnum(entry.getKey());
            buf.writeBoolean(entry.getValue());
        }
    }

    public static RuleSettings readFromBuf(FriendlyByteBuf buf, String groupName) {
        RuleSettings settings = new RuleSettings(groupName, false);
        int activeCount = buf.readVarInt();
        for (int i = 0; i < activeCount; i++) {
            Rule rule = buf.readEnum(Rule.class);
            boolean enabled = buf.readBoolean();
            settings.activeRules.put(rule, enabled);
        }
        int frozenCount = buf.readVarInt();
        for (int i = 0; i < frozenCount; i++) {
            Rule rule = buf.readEnum(Rule.class);
            boolean enabled = buf.readBoolean();
            settings.frozenStates.put(rule, enabled);
        }
        return settings;
    }

    @Override
    public void onFreezeChanged(Rule rule, boolean nowFrozen) {
        if (!isRuleAllowed(rule)) return;
        if (nowFrozen) {
            if (activeRules.containsKey(rule)) {
                frozenStates.put(rule, activeRules.remove(rule));
            }
        } else {
            if (frozenStates.containsKey(rule)) {
                activeRules.put(rule, frozenStates.remove(rule));
            } else {
                boolean def = "owner".equals(groupName);
                activeRules.put(rule, def);
            }
        }
    }

    private void syncWithCurrentFreezeState() {
        for (Rule rule : Rule.values()) {
            if (!isRuleAllowed(rule)) {
                activeRules.remove(rule);
                frozenStates.remove(rule);
                continue;
            }

            boolean isCurrentlyFrozen = FrozenRules.isFrozen(rule);
            if (isCurrentlyFrozen) {
                if (!frozenStates.containsKey(rule) && activeRules.containsKey(rule)) {
                    frozenStates.put(rule, activeRules.remove(rule));
                }
            } else {
                if (frozenStates.containsKey(rule)) {
                    activeRules.put(rule, frozenStates.remove(rule));
                } else if (!activeRules.containsKey(rule)) {
                    boolean def = "owner".equals(groupName);
                    activeRules.put(rule, def);
                }
            }
        }
    }

    @Override
    public void close() {
        if (closed) return;
        FrozenRules.removeObserver(this);
        closed = true;
        registered = false;
    }

    @Deprecated(forRemoval = true)
    public void dispose() {
        close();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RuleSettings other)) return false;
        return activeRules.equals(other.activeRules) && frozenStates.equals(other.frozenStates);
    }

    @Override
    public int hashCode() {
        return Objects.hash(activeRules, frozenStates);
    }
}