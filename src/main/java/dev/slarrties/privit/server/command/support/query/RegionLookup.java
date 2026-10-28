package dev.slarrties.privit.server.command.support.query;

import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.Region;
import dev.slarrties.privit.server.region.RegionManager;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import java.util.List;
import java.util.UUID;
import java.util.Optional;
import java.util.ArrayList;
import java.util.Comparator;

public final class RegionLookup {

    public record LocatedRegion(Region region, ServerLevel level) {
        public RegionManager manager() {
            return WorldRegistry.get(level).getRegionManager();
        }
    }

    private RegionLookup() {}

    public static Optional<LocatedRegion> findById(MinecraftServer server, UUID regionId) {
        for (ServerLevel level : server.getAllLevels()) {
            Optional<Region> region = WorldRegistry.get(level)
                    .getRegionManager()
                    .getById(regionId);
            if (region.isPresent()) {
                return Optional.of(new LocatedRegion(region.get(), level));
            }
        }
        return Optional.empty();
    }

    public static List<LocatedRegion> findOwned(MinecraftServer server, UUID playerUuid) {
        List<LocatedRegion> result = new ArrayList<>();

        for (ServerLevel level : server.getAllLevels()) {
            RegionManager manager = WorldRegistry.get(level).getRegionManager();

            for (Region region : manager.getAll()) {
                if (region.isOwner(playerUuid)) {
                    result.add(new LocatedRegion(region, level));
                }
            }
        }

        result.sort(Comparator
                .comparing((LocatedRegion o) -> o.region().name(), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(o -> o.region().id()));
        return result;
    }
}