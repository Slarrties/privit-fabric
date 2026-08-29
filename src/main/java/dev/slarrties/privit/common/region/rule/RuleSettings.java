package dev.slarrties.privit.common.region.rule;

import dev.slarrties.privit.common.config.ConfigManager;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;

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
        syncWithCurrentFreezeState();
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

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        NbtCompound active = new NbtCompound();
        NbtCompound frozen = new NbtCompound();

        activeRules.forEach((r, v) -> active.putBoolean(r.name(), v));
        frozenStates.forEach((r, v) -> frozen.putBoolean(r.name(), v));

        nbt.put("active", active);
        nbt.put("frozen", frozen);
        return nbt;
    }

    public static RuleSettings fromNbt(NbtCompound nbt, String groupName) {
        RuleSettings settings = new RuleSettings(groupName);

        NbtCompound activeTag = nbt.getCompound("active");
        for (String key : activeTag.getKeys()) {
            try {
                Rule rule = Rule.valueOf(key);
                settings.activeRules.put(rule, activeTag.getBoolean(key));
            } catch (Exception ignored) {}
        }

        NbtCompound frozenTag = nbt.getCompound("frozen");
        for (String key : frozenTag.getKeys()) {
            try {
                Rule rule = Rule.valueOf(key);
                settings.frozenStates.put(rule, frozenTag.getBoolean(key));
            } catch (Exception ignored) {}
        }

        settings.syncWithCurrentFreezeState();
        return settings;
    }

    public void writeToBuf(PacketByteBuf buf) {
        buf.writeVarInt(activeRules.size());
        for (Map.Entry<Rule, Boolean> entry : activeRules.entrySet()) {
            buf.writeEnumConstant(entry.getKey());
            buf.writeBoolean(entry.getValue());
        }

        buf.writeVarInt(frozenStates.size());
        for (Map.Entry<Rule, Boolean> entry : frozenStates.entrySet()) {
            buf.writeEnumConstant(entry.getKey());
            buf.writeBoolean(entry.getValue());
        }
    }

    public static RuleSettings readFromBuf(PacketByteBuf buf, String groupName) {
        RuleSettings settings = new RuleSettings(groupName);

        int activeCount = buf.readVarInt();
        for (int i = 0; i < activeCount; i++) {
            Rule rule = buf.readEnumConstant(Rule.class);
            boolean enabled = buf.readBoolean();
            settings.activeRules.put(rule, enabled);
        }

        int frozenCount = buf.readVarInt();
        for (int i = 0; i < frozenCount; i++) {
            Rule rule = buf.readEnumConstant(Rule.class);
            boolean enabled = buf.readBoolean();
            settings.frozenStates.put(rule, enabled);
        }

        settings.syncWithCurrentFreezeState();
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