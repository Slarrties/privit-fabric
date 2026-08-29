package dev.slarrties.privit.common.region.gui.state;

import dev.slarrties.privit.common.config.ConfigManager;
import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.RegionGroups;
import dev.slarrties.privit.common.region.RegionPlayerGroup;

import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.network.PacketByteBuf;

import java.util.*;

public final class RegionGuiState {

    private final UUID id;
    private String name;
    private BlockBox realBounds;
    private BlockBox draftBounds;
    private List<BlockBox> conflictBounds = Collections.emptyList();
    private BlockPos pivotPos;
    private Color color;
    private RegionGroups groups;
    private boolean isCreated;
    private boolean isChanged;
    private boolean isAreaLimitExceeded;

    private RegionGuiState(UUID id) {
        this.id = Objects.requireNonNull(id);
    }

    public static RegionGuiState createNew(UUID id, UUID ownerUuid, BlockPos pivotPos, String ownerName) {
        RegionGuiState state = new RegionGuiState(id);

        state.name = (ownerName != null && !ownerName.isBlank())
                ? ownerName + "'s region"
                : "New region";
        state.pivotPos = pivotPos;
        state.realBounds = null;
        state.draftBounds = BlockBox.create(
                pivotPos.add(-2, -2, -2),
                pivotPos.add(2, 2, 2)
        );
        state.conflictBounds = Collections.emptyList();
        state.color = Color.getDefault();
        state.isCreated = false;
        state.isChanged = false;
        state.isAreaLimitExceeded = false;
        state.groups = RegionGroups.create(ownerUuid);

        return state;
    }

    public static RegionGuiState createBlank(UUID id) {
        return new RegionGuiState(id);
    }

    public void restoreCommitted(String name, BlockBox bounds, Color color, RegionGroups groups) {
        this.name = name;
        this.realBounds = this.draftBounds = bounds;
        this.conflictBounds = Collections.emptyList();
        this.color = color;
        this.groups = groups;
        this.isCreated = true;
        this.isChanged = false;
        this.isAreaLimitExceeded = false;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public BlockBox getRealBounds() { return realBounds; }
    public BlockBox getDraftBounds() { return draftBounds; }
    public List<BlockBox> getConflictBounds() { return Collections.unmodifiableList(conflictBounds); }
    public BlockPos getPivotPos() { return pivotPos; }
    public Color getColor() { return color; }
    public RegionGroups getGroups() { return groups; }
    public boolean isCreated() { return isCreated; }
    public boolean isChanged() { return isChanged; }
    public boolean isAreaLimitExceeded() { return isAreaLimitExceeded; }

    public boolean setName(String newName) {
        if (newName == null || newName.isBlank() || newName.length() > 32) {
            return false;
        }
        this.name = newName;
        return true;
    }

    public void setColor(Color newColor) {
        this.color = (newColor != null) ? newColor : Color.getDefault();
    }

    public void setDraftBounds(BlockBox newBounds) {
        if (newBounds == null) return;
        this.draftBounds = newBounds;

        int maxArea = ConfigManager.get().regionLimits.maxArea;
        this.isAreaLimitExceeded = maxArea > 0 && calculateVolume(this.draftBounds) > maxArea;
    }

    private long calculateVolume(BlockBox bounds) {
        long sizeX = bounds.getMaxX() - bounds.getMinX() + 1L;
        long sizeY = bounds.getMaxY() - bounds.getMinY() + 1L;
        long sizeZ = bounds.getMaxZ() - bounds.getMinZ() + 1L;
        return sizeX * sizeY * sizeZ;
    }

    public void setConflictBounds(List<BlockBox> conflictBounds) {
        this.conflictBounds = conflictBounds != null
                ? List.copyOf(conflictBounds)
                : Collections.emptyList();
    }

    public void clearConflictBounds() {
        this.conflictBounds = Collections.emptyList();
    }

    public void setPivotPos(BlockPos newPos) {
        this.pivotPos = newPos;
    }

    public void applyGroups(RegionGroups newGroups) {
        Objects.requireNonNull(newGroups, "[RegionGuiState::applyGroups] newGroups cannot be null");
        if (this.groups.equals(newGroups)) return;
        this.groups = newGroups;
    }

    public void setChanged(boolean changed) {
        this.isChanged = changed;
    }

    public boolean hasRealBounds() {
        return realBounds != null;
    }

    public void writeToBuf(PacketByteBuf buf) {
        buf.writeUuid(id);
        buf.writeString(name, 64);
        buf.writeBlockPos(pivotPos);

        buf.writeBoolean(hasRealBounds());
        if (hasRealBounds()) {
            buf.writeInt(realBounds.getMinX());
            buf.writeInt(realBounds.getMinY());
            buf.writeInt(realBounds.getMinZ());
            buf.writeInt(realBounds.getMaxX());
            buf.writeInt(realBounds.getMaxY());
            buf.writeInt(realBounds.getMaxZ());
        }

        buf.writeInt(draftBounds.getMinX());
        buf.writeInt(draftBounds.getMinY());
        buf.writeInt(draftBounds.getMinZ());
        buf.writeInt(draftBounds.getMaxX());
        buf.writeInt(draftBounds.getMaxY());
        buf.writeInt(draftBounds.getMaxZ());

        buf.writeVarInt(conflictBounds.size());
        for (BlockBox box : conflictBounds) {
            buf.writeInt(box.getMinX());
            buf.writeInt(box.getMinY());
            buf.writeInt(box.getMinZ());
            buf.writeInt(box.getMaxX());
            buf.writeInt(box.getMaxY());
            buf.writeInt(box.getMaxZ());
        }

        buf.writeString(color.getCode(), 16);
        groups.writeToBuf(buf);
        buf.writeBoolean(isCreated);
        buf.writeBoolean(isChanged);
        buf.writeBoolean(isAreaLimitExceeded);
    }

    public static RegionGuiState readFromBuf(PacketByteBuf buf) {
        UUID id = buf.readUuid();
        String name = buf.readString(64);
        BlockPos pivotPos = buf.readBlockPos();

        boolean hasRealBounds = buf.readBoolean();
        BlockBox realBounds = null;
        if (hasRealBounds) {
            realBounds = BlockBox.create(
                    new BlockPos(buf.readInt(), buf.readInt(), buf.readInt()),
                    new BlockPos(buf.readInt(), buf.readInt(), buf.readInt())
            );
        }

        BlockBox draftBounds = BlockBox.create(
                new BlockPos(buf.readInt(), buf.readInt(), buf.readInt()),
                new BlockPos(buf.readInt(), buf.readInt(), buf.readInt())
        );

        int conflictCount = buf.readVarInt();
        List<BlockBox> conflictBounds = new ArrayList<>(conflictCount);
        for (int i = 0; i < conflictCount; i++) {
            conflictBounds.add(BlockBox.create(
                    new BlockPos(buf.readInt(), buf.readInt(), buf.readInt()),
                    new BlockPos(buf.readInt(), buf.readInt(), buf.readInt())
            ));
        }

        Color color = Color.fromCode(buf.readString(16));

        int groupCount = buf.readVarInt();
        List<RegionPlayerGroup> groups = new ArrayList<>(groupCount);
        for (int i = 0; i < groupCount; i++) {
            groups.add(RegionPlayerGroup.readFromBuf(buf));
        }

        boolean isCreated = buf.readBoolean();
        boolean isChanged = buf.readBoolean();
        boolean isAreaLimitExceeded = buf.readBoolean();

        RegionGuiState state = new RegionGuiState(id);
        state.name = name;
        state.realBounds = realBounds;
        state.draftBounds = draftBounds;
        state.conflictBounds = conflictBounds;
        state.pivotPos = pivotPos;
        state.color = color;
        state.groups = RegionGroups.from(groups);
        state.isCreated = isCreated;
        state.isChanged = isChanged;
        state.isAreaLimitExceeded = isAreaLimitExceeded;
        return state;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RegionGuiState that)) return false;
        return id.equals(that.id)
                && Objects.equals(name, that.name)
                && Objects.equals(realBounds, that.realBounds)
                && Objects.equals(draftBounds, that.draftBounds)
                && Objects.equals(color, that.color)
                && Objects.equals(groups, that.groups);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, realBounds, draftBounds, color, groups);
    }
}