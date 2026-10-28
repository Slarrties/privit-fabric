package dev.slarrties.privit.common.block.entity;

import dev.slarrties.privit.common.registry.BlockEntityRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.UUID;

public class RegionTableBlockEntity extends BlockEntity {

    private UUID regionId = null;

    public RegionTableBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.REGION_TABLE, pos, state);
    }

    public void setRegionId(UUID id) {
        this.regionId = id;
        setChanged();
    }

    public UUID getRegionId() {
        return regionId;
    }

    public boolean hasRegion() {
        return regionId != null;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (regionId != null) {
            output.putString("RegionId", regionId.toString());
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);

        String uuidStr = input.getStringOr("RegionId", "");

        if (!uuidStr.isEmpty()) {
            try {
                regionId = UUID.fromString(uuidStr);
            } catch (IllegalArgumentException e) {
                regionId = null;
            }
        } else {
            regionId = null;
        }
    }
}