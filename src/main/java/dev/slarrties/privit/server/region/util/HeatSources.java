package dev.slarrties.privit.server.region.util;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.region.protection.AssociatedRule;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.Set;

@AssociatedRule(Rule.USE_FLUIDS)
public final class HeatSources {

    private HeatSources() {}

    // TODO: all heat blocks in list?
    public static final Set<Block> HEAT_SOURCES = Set.of(
            Blocks.TORCH,
            Blocks.WALL_TORCH,
            Blocks.LANTERN,
            Blocks.JACK_O_LANTERN,
            Blocks.GLOWSTONE,
            Blocks.SHROOMLIGHT,
            Blocks.SEA_LANTERN,
            Blocks.OCHRE_FROGLIGHT,
            Blocks.VERDANT_FROGLIGHT,
            Blocks.PEARLESCENT_FROGLIGHT,
            Blocks.BEACON,
            Blocks.END_ROD,
            Blocks.CAMPFIRE
    );

    public static boolean isHeatSource(Block block) {
        return HEAT_SOURCES.contains(block);
    }
}