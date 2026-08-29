package dev.slarrties.privit.common.registry;

import dev.slarrties.privit.PrivitMod;
import dev.slarrties.privit.common.block.RegionTableBlock;

import net.minecraft.item.Item;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.block.MapColor;
import net.minecraft.registry.Registry;
import net.minecraft.registry.Registries;
import net.minecraft.block.AbstractBlock;
import net.minecraft.sound.BlockSoundGroup;

public final class BlocksRegistry {
    public static final Block REGION_TABLE = new RegionTableBlock(
            AbstractBlock.Settings.create()
                    .mapColor(MapColor.IRON_GRAY)
                    .strength(5.0f, 1200.0f)
                    .sounds(BlockSoundGroup.METAL)
                    .nonOpaque()
    );

    public static void register() {
        Registry.register(Registries.BLOCK, PrivitMod.id("region_table"), REGION_TABLE);
        Registry.register(Registries.ITEM, PrivitMod.id("region_table"), new BlockItem(REGION_TABLE, new Item.Settings()));
    }
}
