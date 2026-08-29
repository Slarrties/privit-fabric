package dev.slarrties.privit.server.command.support.query;

import dev.slarrties.privit.server.region.Region;
import dev.slarrties.privit.server.region.RegionManager;
import dev.slarrties.privit.server.world.WorldRegistry;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;

import java.util.List;
import java.util.UUID;
import java.util.Optional;
import java.util.ArrayList;
import java.util.Comparator;

public final class RegionLookup {

    public record LocatedRegion(Region region, ServerWorld world) {
        public RegionManager manager() {
            return WorldRegistry.get(world).getRegionManager();
        }
    }

    private RegionLookup() {}

    public static Optional<LocatedRegion> findById(MinecraftServer server, UUID regionId) {
        for (ServerWorld world : server.getWorlds()) {
            Optional<Region> region = WorldRegistry.get(world)
                    .getRegionManager()
                    .getById(regionId);
            if (region.isPresent()) {
                return Optional.of(new LocatedRegion(region.get(), world));
            }
        }
        return Optional.empty();
    }

    public static List<LocatedRegion> findOwned(MinecraftServer server, UUID playerUuid) {
        List<LocatedRegion> result = new ArrayList<>();

        for (ServerWorld world : server.getWorlds()) {
            RegionManager manager = WorldRegistry.get(world).getRegionManager();
            for (Region region : manager.getAll()) {
                if (region.isOwner(playerUuid)) {
                    result.add(new LocatedRegion(region, world));
                }
            }
        }

        result.sort(Comparator
                .comparing((LocatedRegion o) -> o.region().name(), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(o -> o.region().id()));

        return result;
    }
}