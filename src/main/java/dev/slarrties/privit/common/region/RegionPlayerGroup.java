package dev.slarrties.privit.common.region;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.region.rule.RuleSettings;

import net.minecraft.nbt.Tag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

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
        if (isVisitorsGroup()) throw new UnsupportedOperationException("[RegionPlayerGroup] You can't add members to the visitors group.");

        members.add(uuid);
    }

    public void removeMember(UUID uuid) {
        if (isVisitorsGroup() || (isOwnerGroup() && members.size() <= 1)) throw new UnsupportedOperationException("[RegionPlayerGroup] impermissible changes to group members");

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

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putString("name", name);

        ListTag membersList = new ListTag();
        for (UUID uuid : members) {
            membersList.add(new IntArrayTag(new int[]{
                    (int) (uuid.getMostSignificantBits() >> 32),
                    (int) uuid.getMostSignificantBits(),
                    (int) (uuid.getLeastSignificantBits() >> 32),
                    (int) uuid.getLeastSignificantBits()
            }));
        }
        tag.put("members", membersList);
        tag.put("rules", rules.toNbt());

        return tag;
    }

    public static RegionPlayerGroup fromNbt(CompoundTag tag) {
        String name = tag.getStringOr("name", "");
        if (name.isEmpty()) {
            throw new IllegalArgumentException("[RegionPlayerGroup::fromNbt] Missing group name");
        }

        Set<UUID> members = new HashSet<>();
        ListTag list = tag.getListOrEmpty("members");

        for (Tag e : list) {
            if (!(e instanceof IntArrayTag arr) || arr.getAsIntArray().length != 4) {
                throw new IllegalStateException(
                        "[RegionPlayerGroup::fromNbt] Invalid member entry in group '" + name + "': " + e
                );
            }
            int[] ints = arr.getAsIntArray();
            long most  = ((long) ints[0] << 32) | (ints[1] & 0xFFFFFFFFL);
            long least = ((long) ints[2] << 32) | (ints[3] & 0xFFFFFFFFL);
            members.add(new UUID(most, least));
        }

        String context = name.equalsIgnoreCase("owner") ? "owner"
                : name.equalsIgnoreCase("visitors") ? "visitors"
                : "custom";
        CompoundTag rulesTag = tag.getCompoundOrEmpty("rules");
        RuleSettings rules = RuleSettings.fromNbt(rulesTag, context);

        return new RegionPlayerGroup(name, members, rules);
    }

    public void writeToBuf(FriendlyByteBuf buf) {
        buf.writeUtf(name, 64);
        buf.writeVarInt(members.size());
        members.forEach(buf::writeUUID);
        rules.writeToBuf(buf);
    }

    public static RegionPlayerGroup readFromBuf(FriendlyByteBuf buf) {
        String name = buf.readUtf(64);
        int count = buf.readVarInt();
        Set<UUID> members = new HashSet<>(count);
        for (int i = 0; i < count; i++) members.add(buf.readUUID());

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