package dev.slarrties.privit.common.registry;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.Items;

public class CreativeTabsRegistry {
    public static void register() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(entries -> {
            entries.add(Items.CRAFTING_TABLE);
            entries.add(BlocksRegistry.REGION_TABLE);
        });
    }

    private CreativeTabsRegistry() {}
}
