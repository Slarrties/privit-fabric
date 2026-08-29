package dev.slarrties.privit.common.block.entity;

import dev.slarrties.privit.common.registry.BlockEntityRegistry;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;

import java.util.UUID;

public class RegionTableBlockEntity extends BlockEntity {

    private UUID regionId = null;

    public RegionTableBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.REGION_TABLE, pos, state);
    }

    public void setRegionId(UUID id) {
        this.regionId = id;
        markDirty();
    }

    public UUID getRegionId() {
        return regionId;
    }

    public boolean hasRegion() {
        return regionId != null;
    }

    @Override
    protected void writeNbt(NbtCompound nbt, WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        if (regionId != null) {
            nbt.putUuid("RegionId", regionId);
        }
    }

    @Override
    public void readNbt(NbtCompound nbt, WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        regionId = nbt.containsUuid("RegionId") ? nbt.getUuid("RegionId") : null;
    }
}