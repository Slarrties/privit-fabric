package dev.slarrties.privit.common.region.gui.state;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.RegionGroups;
import dev.slarrties.privit.common.config.ConfigManager;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.*;

public final class RegionGuiState {

    private final UUID id;
    private String name;
    private BoundingBox realBounds;
    private BoundingBox draftBounds;
    private List<BoundingBox> conflictBounds = Collections.emptyList();
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
        state.draftBounds = new BoundingBox(
                pivotPos.getX() - 2, pivotPos.getY() - 2, pivotPos.getZ() - 2,
                pivotPos.getX() + 2, pivotPos.getY() + 2, pivotPos.getZ() + 2
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

    public void restoreCommitted(String name, BoundingBox bounds, Color color, RegionGroups groups) {
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
    public BoundingBox getRealBounds() { return realBounds; }
    public BoundingBox getDraftBounds() { return draftBounds; }
    public List<BoundingBox> getConflictBounds() { return Collections.unmodifiableList(conflictBounds); }
    public BlockPos getPivotPos() { return pivotPos; }
    public Color getColor() { return color; }
    public RegionGroups getGroups() { return groups; }
    public boolean isCreated() { return isCreated; }
    public boolean isChanged() { return isChanged; }
    public boolean isAreaLimitExceeded() { return isAreaLimitExceeded; }
    public boolean hasRealBounds() { return realBounds != null; }

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

    public void setDraftBounds(BoundingBox newBounds) {
        if (newBounds == null) return;
        this.draftBounds = newBounds;

        int maxArea = ConfigManager.get().regionLimits.maxArea;
        this.isAreaLimitExceeded = maxArea > 0 && calculateVolume(this.draftBounds) > maxArea;
    }

    private long calculateVolume(BoundingBox bounds) {
        long sizeX = bounds.maxX() - bounds.minX() + 1L;
        long sizeY = bounds.maxY() - bounds.minY() + 1L;
        long sizeZ = bounds.maxZ() - bounds.minZ() + 1L;
        return sizeX * sizeY * sizeZ;
    }

    public void setConflictBounds(List<BoundingBox> conflictBounds) {
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

    public void writeToBuf(FriendlyByteBuf buf) {
        buf.writeUUID(id);
        buf.writeUtf(name, 64);
        buf.writeBlockPos(pivotPos);
        buf.writeBoolean(hasRealBounds());
        if (hasRealBounds()) {
            buf.writeInt(realBounds.minX());
            buf.writeInt(realBounds.minY());
            buf.writeInt(realBounds.minZ());
            buf.writeInt(realBounds.maxX());
            buf.writeInt(realBounds.maxY());
            buf.writeInt(realBounds.maxZ());
        }
        buf.writeInt(draftBounds.minX());
        buf.writeInt(draftBounds.minY());
        buf.writeInt(draftBounds.minZ());
        buf.writeInt(draftBounds.maxX());
        buf.writeInt(draftBounds.maxY());
        buf.writeInt(draftBounds.maxZ());

        buf.writeVarInt(conflictBounds.size());
        for (BoundingBox box : conflictBounds) {
            buf.writeInt(box.minX());
            buf.writeInt(box.minY());
            buf.writeInt(box.minZ());
            buf.writeInt(box.maxX());
            buf.writeInt(box.maxY());
            buf.writeInt(box.maxZ());
        }

        buf.writeUtf(color.getCode(), 16);
        groups.writeToBuf(buf);

        buf.writeBoolean(isCreated);
        buf.writeBoolean(isChanged);
        buf.writeBoolean(isAreaLimitExceeded);
    }

    public static RegionGuiState readFromBuf(FriendlyByteBuf buf) {
        UUID id = buf.readUUID();
        String name = buf.readUtf(64);
        BlockPos pivotPos = buf.readBlockPos();

        boolean hasRealBounds = buf.readBoolean();
        BoundingBox realBounds = null;
        if (hasRealBounds) {
            realBounds = new BoundingBox(
                    buf.readInt(), buf.readInt(), buf.readInt(),
                    buf.readInt(), buf.readInt(), buf.readInt()
            );
        }

        BoundingBox draftBounds = new BoundingBox(
                buf.readInt(), buf.readInt(), buf.readInt(),
                buf.readInt(), buf.readInt(), buf.readInt()
        );

        int conflictCount = buf.readVarInt();
        List<BoundingBox> conflictBounds = new ArrayList<>(conflictCount);
        for (int i = 0; i < conflictCount; i++) {
            conflictBounds.add(new BoundingBox(
                    buf.readInt(), buf.readInt(), buf.readInt(),
                    buf.readInt(), buf.readInt(), buf.readInt()
            ));
        }

        Color color = Color.fromCode(buf.readUtf(16));
        RegionGroups groups = RegionGroups.readFromBuf(buf);

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
        state.groups = groups;
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