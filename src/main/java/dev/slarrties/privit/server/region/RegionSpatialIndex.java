package dev.slarrties.privit.server.region;

import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

final class RegionSpatialIndex {

    private final Map<Long, Set<UUID>> chunkToRegions = new ConcurrentHashMap<>();

    void index(Region region) {
        forEachChunk(region.bounds(), (cx, cz) -> {
            long key = ChunkPos.toLong(cx, cz);
            chunkToRegions.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet())
                    .add(region.id());
        });
    }

    void unindex(Region region) {
        forEachChunk(region.bounds(), (cx, cz) -> {
            long key = ChunkPos.toLong(cx, cz);
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

    List<BlockBox> computeConflictBounds(
            BlockBox candidateBounds,
            UUID ignoreRegionId,
            Map<UUID, Region> regions
    ) {
        if (candidateBounds == null) return List.of();

        List<BlockBox> conflicts = new ArrayList<>();
        Set<UUID> checked = new HashSet<>();

        forEachChunk(candidateBounds, (cx, cz) -> {
            Set<UUID> idsInChunk = chunkToRegions.get(ChunkPos.toLong(cx, cz));
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

    private static void forEachChunk(BlockBox box, ChunkConsumer consumer) {
        int minX = box.getMinX() >> 4;
        int minZ = box.getMinZ() >> 4;
        int maxX = box.getMaxX() >> 4;
        int maxZ = box.getMaxZ() >> 4;

        for (int cx = minX; cx <= maxX; cx++) {
            for (int cz = minZ; cz <= maxZ; cz++) {
                consumer.accept(cx, cz);
            }
        }
    }

    private static Optional<BlockBox> intersection(BlockBox a, BlockBox b) {
        int minX = Math.max(a.getMinX(), b.getMinX());
        int minY = Math.max(a.getMinY(), b.getMinY());
        int minZ = Math.max(a.getMinZ(), b.getMinZ());
        int maxX = Math.min(a.getMaxX(), b.getMaxX());
        int maxY = Math.min(a.getMaxY(), b.getMaxY());
        int maxZ = Math.min(a.getMaxZ(), b.getMaxZ());

        if (minX > maxX || minY > maxY || minZ > maxZ) {
            return Optional.empty();
        }

        return Optional.of(BlockBox.create(
                new BlockPos(minX, minY, minZ),
                new BlockPos(maxX, maxY, maxZ)
        ));
    }

    @FunctionalInterface
    private interface ChunkConsumer {
        void accept(int chunkX, int chunkZ);
    }
}