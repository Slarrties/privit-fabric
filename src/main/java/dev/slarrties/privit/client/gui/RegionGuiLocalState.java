package dev.slarrties.privit.client.gui;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.RegionGroups;
import dev.slarrties.privit.common.region.gui.state.RegionGuiState;
import dev.slarrties.privit.common.network.payload.s2c.RegionGuiUpdateS2CPacket;

import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.client.MinecraftClient;

import java.util.List;
import java.util.UUID;

public record RegionGuiLocalState(
        UUID id,
        String name,
        BlockBox realBounds,
        BlockBox draftBounds,
        List<BlockBox> conflictBounds,
        BlockPos pivotPos,
        Color color,
        RegionGroups groups,
        boolean isCreated,
        boolean isChanged,
        boolean isAreaLimitExceeded
) {

    public static RegionGuiLocalState from(RegionGuiState state) {
        return new RegionGuiLocalState(
                state.getId(),
                state.getName(),
                state.getRealBounds(),
                state.getDraftBounds(),
                state.getConflictBounds(),
                state.getPivotPos(),
                state.getColor(),
                state.getGroups(),
                state.isCreated(),
                state.isChanged(),
                state.isAreaLimitExceeded()
        );
    }

    public RegionGuiLocalState withUpdate(RegionGuiUpdateS2CPacket packet) {
        return new RegionGuiLocalState(
                packet.regionId(),
                packet.name().orElse(this.name),
                packet.realBounds().orElse(this.realBounds),
                packet.draftBounds().orElse(this.draftBounds),
                packet.conflictBounds().orElse(this.conflictBounds),
                packet.pivotPos().orElse(this.pivotPos),
                packet.color().orElse(this.color),
                packet.groups().orElse(this.groups),
                packet.isCreated().orElse(this.isCreated),
                packet.isChanged(),
                packet.isAreaLimitExceeded().orElse(this.isAreaLimitExceeded)
        );
    }

    public RegionGuiState toRegionScreenState() {
        RegionGuiState state = RegionGuiState.createNew(
                this.id(),
                this.getOwnerUuid(),
                this.pivotPos(),
                this.getOwnerName()
        );

        state.setName(this.name());
        state.setColor(this.color());
        state.setDraftBounds(this.draftBounds());
        state.setConflictBounds(this.conflictBounds());
        state.setPivotPos(this.pivotPos());
        state.applyGroups(this.groups());

        return state;
    }

    // TODO: remove this?
    public UUID getOwnerUuid() {
        return groups.findByName("owner")
                .flatMap(owner -> owner.getMembers().stream().findFirst())
                .orElseThrow(() -> new IllegalStateException("Owner group not found or empty"));
    }

    // TODO: change to NameProvider?
    public String getOwnerName() {
        return MinecraftClient.getInstance().player != null
                ? MinecraftClient.getInstance().player.getName().getString()
                : "Unknown";
    }
}