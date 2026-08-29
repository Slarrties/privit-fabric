package dev.slarrties.privit.common.registry;

import dev.slarrties.privit.PrivitMod;
import dev.slarrties.privit.common.block.entity.RegionTableBlockEntity;

import net.minecraft.registry.Registry;
import net.minecraft.registry.Registries;
import net.minecraft.block.entity.BlockEntityType;

public final class BlockEntityRegistry {
    public static final BlockEntityType<RegionTableBlockEntity> REGION_TABLE =
            BlockEntityType.Builder.create(RegionTableBlockEntity::new, BlocksRegistry.REGION_TABLE).build(null);

    public static void register() {
        Registry.register(Registries.BLOCK_ENTITY_TYPE, PrivitMod.id("region_table"), REGION_TABLE);
    }
}
