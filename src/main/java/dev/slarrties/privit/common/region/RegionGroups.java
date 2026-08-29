package dev.slarrties.privit.common.region;

import dev.slarrties.privit.common.region.rule.RuleSettings;

import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;

import java.util.*;

public final class RegionGroups {

    private static final int MAX_GROUPS = 20;
    private final List<RegionPlayerGroup> groups;

    private static final Set<String> FORBIDDEN_NAMES = Set.of(
            "owner", "owners",
            "visitor", "visitors"
    );

    public static boolean isNameForbidden(String name) {
        if (name == null || name.isBlank()) return true;
        return FORBIDDEN_NAMES.contains(name.trim().toLowerCase(Locale.ROOT));
    }

    private RegionGroups(List<RegionPlayerGroup> validatedGroups) {
        this.groups = List.copyOf(Objects.requireNonNull(validatedGroups));
        validateNoDuplicateMembers();
    }

    public static RegionGroups create(UUID ownerUuid) {
        Objects.requireNonNull(ownerUuid, "[RegionGroups] ownerUuid cannot be null");

        List<RegionPlayerGroup> initial = List.of(
                new RegionPlayerGroup("owner", Set.of(ownerUuid), new RuleSettings("owner")),
                new RegionPlayerGroup("visitors", Collections.emptySet(), new RuleSettings("visitors"))
        );

        return new RegionGroups(initial);
    }

    public static RegionGroups from(Collection<RegionPlayerGroup> groups) {
        if (groups == null || groups.isEmpty())
            throw new IllegalArgumentException("[RegionGroups::from] Groups collection cannot be null or empty");

        if (groups.size() > MAX_GROUPS)
            throw new IllegalArgumentException("[RegionGroups::from] Too many groups. " +
                    "Maximum allowed is " + MAX_GROUPS + ", got " + groups.size());

        List<RegionPlayerGroup> validated = new ArrayList<>(groups.size());
        boolean hasOwner = false;
        boolean hasVisitors = false;
        Set<String> nameSet = new HashSet<>();

        for (RegionPlayerGroup group : groups) {
            if (group == null) continue;

            String name = group.getName();
            String lowerName = name.toLowerCase(Locale.ROOT);

            if (nameSet.contains(lowerName))
                throw new IllegalArgumentException("Duplicate group name: " + name);

            boolean special = name.equalsIgnoreCase("owner") || name.equalsIgnoreCase("visitors");
            if (!special && isNameForbidden(name))
                throw new IllegalArgumentException("[RegionGroups::from] Forbidden group name: " + name);

            nameSet.add(lowerName);

            if (name.equalsIgnoreCase("owner")) hasOwner = true;
            if (name.equalsIgnoreCase("visitors")) hasVisitors = true;
            if (name.equalsIgnoreCase("owner") && group.getMembers().isEmpty())
                throw new IllegalArgumentException("[RegionGroups::from] Owner group cannot be empty");

            validated.add(new RegionPlayerGroup(group));
        }

        if (!hasOwner)
            throw new IllegalArgumentException("[RegionGroups::from] Owner group is required");

        if (!hasVisitors)
            validated.add(new RegionPlayerGroup("visitors", Collections.emptySet(), new RuleSettings("visitors")));

        return new RegionGroups(validated);
    }

    public static RegionGroups copyOf(RegionGroups original) {
        Objects.requireNonNull(original);
        return new RegionGroups(original.groups);
    }

    public List<RegionPlayerGroup> getAll() { return groups; }

    public Optional<RegionPlayerGroup> findByName(String name) {
        if (name == null) return Optional.empty();

        return groups.stream()
                .filter(g -> g.getName().equalsIgnoreCase(name))
                .findFirst();
    }

    public RegionGroups addGroup(RegionPlayerGroup group) {
        Objects.requireNonNull(group);

        if (isNameForbidden(group.getName()))
            throw new IllegalArgumentException("[RegionGroups::addGroup] Forbidden group name: " + group.getName());

        if (group.isOwnerGroup() || group.isVisitorsGroup())
            throw new UnsupportedOperationException("[RegionGroups::addGroup] Use withUpdatedGroup() to modify special groups");

        if (findByName(group.getName()).isPresent())
            throw new IllegalArgumentException("[RegionGroups::addGroup] Group with name '" + group.getName() + "' already exists");

        if (groups.size() >= MAX_GROUPS)
            throw new IllegalArgumentException("[RegionGroups::addGroup]  Cannot add more groups. " +
                    "Maximum number of groups per region is " + MAX_GROUPS + ".");

        List<RegionPlayerGroup> newList = new ArrayList<>(groups);
        newList.add(new RegionPlayerGroup(group));

        return new RegionGroups(newList);
    }

    public RegionGroups removeGroup(String name) {
        if (name == null) return this;
        if (name.equalsIgnoreCase("owner") || name.equalsIgnoreCase("visitors"))
            throw new UnsupportedOperationException("Cannot remove special group: " + name);

        List<RegionPlayerGroup> newList = groups.stream()
                .filter(g -> !g.getName().equalsIgnoreCase(name))
                .toList();

        if (newList.size() == groups.size()) return this;

        return new RegionGroups(newList);
    }

    public RegionGroups renameGroup(String oldName, String newName) {
        if (oldName == null || newName == null || newName.isBlank() || newName.length() > 32) return this;
        if (oldName.equalsIgnoreCase("owner") || oldName.equalsIgnoreCase("visitors"))
            throw new UnsupportedOperationException("Cannot rename special group: " + oldName);

        if (isNameForbidden(newName))
            throw new IllegalArgumentException("[RegionGroups::renameGroup] Forbidden group name: " + newName);

        Optional<RegionPlayerGroup> oldGroupOpt = findByName(oldName);
        if (oldGroupOpt.isEmpty()) return this;
        if (findByName(newName).isPresent())
            throw new IllegalArgumentException("Group with name '" + newName + "' already exists");

        RegionPlayerGroup oldGroup = oldGroupOpt.get();
        RegionPlayerGroup renamed = new RegionPlayerGroup(
                newName,
                oldGroup.getMembers(),
                oldGroup.getRuleSettings()
        );

        List<RegionPlayerGroup> newList = groups.stream()
                .map(g -> g.getName().equalsIgnoreCase(oldName) ? renamed : g)
                .toList();

        return new RegionGroups(newList);
    }

    public RegionGroups updateGroups(Collection<RegionPlayerGroup> newGroups) { return from(newGroups); }

    public RegionGroups withUpdatedGroup(String groupName, RegionPlayerGroup updatedGroup) {
        Objects.requireNonNull(groupName);
        Objects.requireNonNull(updatedGroup);

        if (!groupName.equalsIgnoreCase(updatedGroup.getName()))
            throw new IllegalArgumentException("[RegionGroups::withUpdatedGroup] Group name in parameter and in updatedGroup must match");
        if (groupName.equalsIgnoreCase("owner") && updatedGroup.getMembers().isEmpty())
            throw new IllegalArgumentException("[RegionGroups::withUpdatedGroup] Owner group cannot be empty");

        List<RegionPlayerGroup> newList = groups.stream()
                .map(g -> g.getName().equalsIgnoreCase(groupName) ? new RegionPlayerGroup(updatedGroup) : g)
                .toList();

        return new RegionGroups(newList);
    }

    public NbtCompound toNbt() {
        NbtCompound tag = new NbtCompound();
        NbtList list = new NbtList();

        for (RegionPlayerGroup group : groups)
            list.add(group.toNbt());

        tag.put("groups", list);
        return tag;
    }

    public static RegionGroups fromNbt(NbtCompound tag) {
        if (tag == null || !tag.contains("groups", NbtElement.LIST_TYPE))
            throw new IllegalArgumentException("Invalid NBT for RegionGroups");

        NbtList list = tag.getList("groups", NbtElement.COMPOUND_TYPE);
        List<RegionPlayerGroup> parsed = new ArrayList<>(list.size());

        for (int i = 0; i < list.size(); i++)
            parsed.add(RegionPlayerGroup.fromNbt(list.getCompound(i)));

        return from(parsed);
    }

    public void writeToBuf(PacketByteBuf buf) {
        buf.writeVarInt(groups.size());

        for (RegionPlayerGroup group : groups) {
            group.writeToBuf(buf);
        }
    }

    public static RegionGroups readFromBuf(PacketByteBuf buf) {
        int count = buf.readVarInt();
        List<RegionPlayerGroup> list = new ArrayList<>(count);

        for (int i = 0; i < count; i++)
            list.add(RegionPlayerGroup.readFromBuf(buf));

        return from(list);
    }

    private void validateNoDuplicateMembers() {
        Set<UUID> seen = new HashSet<>();

        for (RegionPlayerGroup group : groups) {
            for (UUID member : group.getMembers()) {
                if (!seen.add(member)) {
                    throw new IllegalArgumentException(
                            "[RegionGroups] Player " + member + " cannot belong to multiple groups in the same region. " +
                                    "Found in group '" + group.getName() + "' (and previously in another group)."
                    );
                }
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RegionGroups that)) return false;
        return groups.equals(that.groups);
    }

    @Override
    public int hashCode() {
        return groups.hashCode();
    }

    @Override
    public String toString() {
        return "RegionGroups{" + groups + '}';
    }
}