package dev.slarrties.privit.server.region.protection;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.region.rule.FrozenRules;
import dev.slarrties.privit.server.region.Region;
import dev.slarrties.privit.server.world.WorldRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public final class RegionPermissionChecker {

    private RegionPermissionChecker() {}

    public static boolean isAllowed(
            @NotNull UUID playerId,
            @NotNull Rule rule,
            @NotNull BlockPos pos,
            @NotNull ServerLevel world
    ) {
        if (FrozenRules.isFrozen(rule)) return true;

        Region region = WorldRegistry.get(world).getRegionManager().getAt(pos).orElse(null);
        if (region == null) return true;

        boolean isAllowed = region.isAllowed(playerId, rule);

//        PrivitMod.LOGGER.info("[RegionPermissionChecker] Player {} rule {} is {} on {}", playerId, rule, isAllowed, pos);

        return isAllowed;
    }

    public static boolean isAllowed(
            @NotNull ServerPlayer player,
            @NotNull Rule rule,
            @NotNull BlockPos pos
    ) {
        return isAllowed(player.getUUID(), rule, pos, player.level());
    }
}