package dev.slarrties.privit.common.registry;

import dev.slarrties.privit.PrivitMod;
import dev.slarrties.privit.common.block.RegionTableBlock;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public final class BlocksRegistry {

    public static final ResourceKey<Block> REGION_TABLE_KEY = ResourceKey.create(Registries.BLOCK, PrivitMod.id("region_table"));
    public static final ResourceKey<Item> REGION_TABLE_ITEM_KEY = ResourceKey.create(Registries.ITEM, PrivitMod.id("region_table"));

    public static final Block REGION_TABLE = new RegionTableBlock(
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(5.0f, 1200.0f)
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .setId(REGION_TABLE_KEY)
    );

    public static void register() {
        Registry.register(BuiltInRegistries.BLOCK, REGION_TABLE_KEY, REGION_TABLE);
        Registry.register(
                BuiltInRegistries.ITEM,
                REGION_TABLE_ITEM_KEY,
                new BlockItem(REGION_TABLE, new Item.Properties().setId(REGION_TABLE_ITEM_KEY))
        );
    }
}