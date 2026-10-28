package dev.slarrties.privit.common.registry;

import dev.slarrties.privit.PrivitMod;
import dev.slarrties.privit.common.block.entity.RegionTableBlockEntity;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;

public final class BlockEntityRegistry {

    public static final BlockEntityType<RegionTableBlockEntity> REGION_TABLE =
            FabricBlockEntityTypeBuilder.create(RegionTableBlockEntity::new, BlocksRegistry.REGION_TABLE).build();

    public static void register() {
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, PrivitMod.id("region_table"), REGION_TABLE);
    }
}