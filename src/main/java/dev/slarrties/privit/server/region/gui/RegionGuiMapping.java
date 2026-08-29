package dev.slarrties.privit.server.region.gui;

import dev.slarrties.privit.server.region.Region;
import dev.slarrties.privit.common.region.gui.state.RegionGuiState;

import net.minecraft.util.math.BlockPos;

public final class RegionGuiMapping {

    private RegionGuiMapping() {}

    public static RegionGuiState fromRegion(Region region, BlockPos pivotPos) {
        RegionGuiState state = RegionGuiState.createBlank(region.id());
        fillFromRegion(state, region);
        state.setPivotPos(pivotPos);
        return state;
    }

    public static void fillFromRegion(RegionGuiState state, Region region) {
        state.restoreCommitted(
                region.name(),
                region.bounds(),
                region.color(),
                region.groups()
        );
    }

    public static Region toRegion(RegionGuiState state) {
        return new Region(
                state.getId(),
                state.getName(),
                state.getDraftBounds(),
                state.getPivotPos(),
                state.getColor(),
                state.getGroups()
        );
    }

    public static void recalculateChanged(RegionGuiState state, Region realRegion) {
        if (!state.isCreated()) {
            state.setChanged(false);
            return;
        }

        if (realRegion == null) {
            state.setChanged(true);
            return;
        }

        state.setChanged(
                !state.getName().equals(realRegion.name())
                        || !state.getDraftBounds().equals(realRegion.bounds())
                        || !state.getColor().equals(realRegion.color())
                        || !state.getGroups().equals(realRegion.groups())
        );
    }
}