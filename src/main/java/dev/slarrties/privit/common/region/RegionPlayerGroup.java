package dev.slarrties.privit.common.region;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.region.rule.RuleSettings;

import net.minecraft.nbt.*;
import net.minecraft.network.PacketByteBuf;

import java.util.*;

public final class RegionPlayerGroup {

    private String name;
    private Set<UUID> members = new HashSet<>();
    private final RuleSettings rules;

    public RegionPlayerGroup(String name, Collection<UUID> members, RuleSettings rules) {
        this.name = Objects.requireNonNull(name);
        if(!members.isEmpty()) this.members.addAll(members);
        this.rules = Objects.requireNonNull(rules);
    }

    public RegionPlayerGroup(RegionPlayerGroup original) {
        this.name = original.name;
        this.members = new HashSet<>(original.members);
        this.rules = new RuleSettings(original.rules);
    }

    public String getName() { return name; }

    public void setName(String newName) { this.name = Objects.requireNonNull(newName); }

    public Set<UUID> getMembers() { return Collections.unmodifiableSet(members); }

    public RuleSettings getRuleSettings() { return rules; }

    public void addMember(UUID uuid) {
        if (isVisitorsGroup()) {
            throw new UnsupportedOperationException("[RegionPlayerGroup] You can't add members to the visitors group.");
        }

        members.add(uuid);
    }

    public void removeMember(UUID uuid) {
        if (isVisitorsGroup() || (isOwnerGroup() && members.size() <= 1)) {
            throw new UnsupportedOperationException("[RegionPlayerGroup] impermissible changes to group members");
        }
        members.remove(uuid);
    }

    public boolean isRuleEnabled(Rule rule) {
        return rules.isEnabled(rule);
    }

    public void setRuleEnabled(Rule rule, boolean enabled) {
        rules.setRuleState(rule, enabled);
    }

    public boolean isOwnerGroup() {
        return name.equalsIgnoreCase("owner");
    }

    public boolean isVisitorsGroup() {
        return name.equalsIgnoreCase("visitors");
    }

    public NbtCompound toNbt() {
        NbtCompound tag = new NbtCompound();
        tag.putString("name", name);

        NbtList membersList = new NbtList();
        for (UUID uuid : members) {
            membersList.add(NbtHelper.fromUuid(uuid));
        }
        tag.put("members", membersList);
        tag.put("rules", rules.toNbt());

        return tag;
    }

    public static RegionPlayerGroup fromNbt(NbtCompound tag) {
        String name = tag.getString("name");

        Set<UUID> members = new HashSet<>();
        NbtList list = tag.getList("members", NbtElement.INT_ARRAY_TYPE);
        for (int i = 0; i < list.size(); i++) {
            NbtElement e = list.get(i);
            if (e instanceof NbtIntArray arr) {
                members.add(NbtHelper.toUuid(arr));
            }
        }

        String context = name.equalsIgnoreCase("owner") ? "owner" :
                name.equalsIgnoreCase("visitors") ? "visitors" : "custom";

        RuleSettings rules = RuleSettings.fromNbt(tag.getCompound("rules"), context);

        return new RegionPlayerGroup(name, members, rules);
    }

    public void writeToBuf(PacketByteBuf buf) {
        buf.writeString(name, 64);
        buf.writeVarInt(members.size());
        members.forEach(buf::writeUuid);
        rules.writeToBuf(buf);
    }

    public static RegionPlayerGroup readFromBuf(PacketByteBuf buf) {
        String name = buf.readString(64);
        int count = buf.readVarInt();
        Set<UUID> members = new HashSet<>(count);
        for (int i = 0; i < count; i++) {
            members.add(buf.readUuid());
        }
        String context = name.equalsIgnoreCase("owner") ? "owner" :
                name.equalsIgnoreCase("visitors") ? "visitors" : "custom";
        RuleSettings rules = RuleSettings.readFromBuf(buf, context);

        return new RegionPlayerGroup(name, members, rules);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RegionPlayerGroup that)) return false;

        return name.equals(that.name) &&
                members.equals(that.members) &&
                rules.equals(that.rules);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, members, rules);
    }
}