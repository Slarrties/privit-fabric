package dev.slarrties.privit.server.region;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

final class RegionSpatialIndex {
    private final Map<Long, Set<UUID>> chunkToRegions = new ConcurrentHashMap<>();

    void index(Region region) {
        forEachChunk(region.bounds(), (cx, cz) -> {
            long key = ChunkPos.pack(cx, cz);
            chunkToRegions.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet())
                    .add(region.id());
        });
    }

    void unindex(Region region) {
        forEachChunk(region.bounds(), (cx, cz) -> {
            long key = ChunkPos.pack(cx, cz);
            Set<UUID> set = chunkToRegions.get(key);
            if (set == null) return;
            set.remove(region.id());
            if (set.isEmpty()) {
                chunkToRegions.remove(key);
            }
        });
    }

    Set<UUID> getRegionIdsInChunk(long chunkKey) {
        return chunkToRegions.getOrDefault(chunkKey, Set.of());
    }

    boolean isMixedChunk(long chunkKey) {
        return getRegionIdsInChunk(chunkKey).size() > 1;
    }

    List<BoundingBox> computeConflictBounds(
            BoundingBox candidateBounds,
            UUID ignoreRegionId,
            Map<UUID, Region> regions
    ) {
        if (candidateBounds == null) return List.of();
        List<BoundingBox> conflicts = new ArrayList<>();
        Set<UUID> checked = new HashSet<>();
        forEachChunk(candidateBounds, (cx, cz) -> {
            Set<UUID> idsInChunk = chunkToRegions.get(ChunkPos.pack(cx, cz));
            if (idsInChunk == null) return;
            for (UUID rid : idsInChunk) {
                if (rid.equals(ignoreRegionId) || !checked.add(rid)) continue;
                Region existing = regions.get(rid);
                if (existing == null) continue;
                intersection(candidateBounds, existing.bounds()).ifPresent(conflicts::add);
            }
        });
        return conflicts;
    }

    private static void forEachChunk(BoundingBox box, ChunkConsumer consumer) {
        int minX = box.minX() >> 4;
        int minZ = box.minZ() >> 4;
        int maxX = box.maxX() >> 4;
        int maxZ = box.maxZ() >> 4;
        for (int cx = minX; cx <= maxX; cx++) {
            for (int cz = minZ; cz <= maxZ; cz++) {
                consumer.accept(cx, cz);
            }
        }
    }

    private static Optional<BoundingBox> intersection(BoundingBox a, BoundingBox b) {
        int minX = Math.max(a.minX(), b.minX());
        int minY = Math.max(a.minY(), b.minY());
        int minZ = Math.max(a.minZ(), b.minZ());
        int maxX = Math.min(a.maxX(), b.maxX());
        int maxY = Math.min(a.maxY(), b.maxY());
        int maxZ = Math.min(a.maxZ(), b.maxZ());
        if (minX > maxX || minY > maxY || minZ > maxZ) {
            return Optional.empty();
        }
        return Optional.of(new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ));
    }

    @FunctionalInterface
    private interface ChunkConsumer {
        void accept(int chunkX, int chunkZ);
    }
}