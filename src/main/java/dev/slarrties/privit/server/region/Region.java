package dev.slarrties.privit.server.region;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.RegionGroups;
import dev.slarrties.privit.common.region.rule.Rule;

import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;

import java.util.*;

public record Region(
        UUID id,
        String name,
        BlockBox bounds,
        BlockPos pivotPos,
        Color color,
        RegionGroups groups
) {

    public static class Builder {
        private UUID id;
        private String name;
        private BlockBox bounds;
        private BlockPos pivotPos;
        private Color color;
        private RegionGroups groups;

        public Builder(BlockPos initialPivotPos, String name, UUID owner) {
            this.id = UUID.randomUUID();
            this.pivotPos = initialPivotPos;
            this.bounds = BlockBox.create(
                    initialPivotPos.add(-2, -2, -2),
                    initialPivotPos.add(2, 2, 2)
            );
            this.color = Color.getDefault();
            this.name = name != null && !name.isBlank() ? name : "Region";
            this.groups = RegionGroups.create(owner);
        }

        public Builder(Region region) {
            this.id = region.id;
            this.name = region.name;
            this.bounds = region.bounds;
            this.pivotPos = region.pivotPos;
            this.color = region.color;
            this.groups = RegionGroups.copyOf(region.groups);
        }

        public Builder id(UUID id)                  { this.id = id; return this; }
        public Builder name(String name)            { this.name = name; return this; }
        public Builder bounds(BlockBox bounds)      { this.bounds = bounds; return this; }
        public Builder pivotPos(BlockPos pos)       { this.pivotPos = pos; return this; }
        public Builder color(Color color)           { this.color = color; return this; }
        public Builder groups(RegionGroups groups) {
            this.groups = Objects.requireNonNull(groups);
            return this;
        }

        public Region build() {
            if (id == null) id = UUID.randomUUID();
            if (groups == null) throw new IllegalStateException("[Region] Groups must be initialized");

            return new Region(id, name, bounds, pivotPos, color, groups);
        }
    }

    public boolean isAllowed(UUID playerUuid, Rule rule) {
        return groups.getAll().stream()
                .filter(g -> g.getMembers().contains(playerUuid))
                .findFirst()
                .or(() -> groups.findByName("visitors"))
                .map(group -> group.isRuleEnabled(rule))
                .orElse(false);
    }

    public boolean isOwner(UUID playerUuid) {
        return groups.findByName("owner")
                .map(g -> g.getMembers().contains(playerUuid))
                .orElse(false);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Region that)) return false;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}