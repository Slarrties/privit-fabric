package dev.slarrties.privit.common.registry;

import net.minecraft.world.item.CreativeModeTabs;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;

public class CreativeTabsRegistry {

    public static void register() {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(output -> {
            output.accept(BlocksRegistry.REGION_TABLE);
        });
    }

    private CreativeTabsRegistry() {}
}