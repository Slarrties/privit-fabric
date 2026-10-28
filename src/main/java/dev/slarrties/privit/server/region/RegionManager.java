package dev.slarrties.privit.server.region;

import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.nbt.RegionNbtPersistence;
import dev.slarrties.privit.server.region.event.PlayerRegionChangeEvent;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.*;
import org.jetbrains.annotations.Nullable;

public final class RegionManager {

    // TODO: sealed interface
    public record OpResult(NotificationType type, List<BoundingBox> conflicts) {
        public boolean isSuccess() {
            return type == NotificationType.REGION_CREATED
                    || type == NotificationType.REGION_UPDATED
                    || type == NotificationType.REGION_DELETED;
        }

        public static OpResult success(NotificationType type) {
            return new OpResult(type, List.of());
        }

        public static OpResult failure(NotificationType type, List<BoundingBox> conflicts) {
            return new OpResult(type, conflicts == null ? List.of() : List.copyOf(conflicts));
        }
    }

    private final ServerLevel level;
    private final RegionNbtPersistence regionStorage;
    private final Map<UUID, Region> regions = new HashMap<>();
    private final RegionSpatialIndex spatialIndex = new RegionSpatialIndex();
    private final RegionValidator validator;

    public RegionManager(ServerLevel level) {
        this.level = level;
        this.regionStorage = new RegionNbtPersistence(level);
        this.validator = new RegionValidator(this);
        regionStorage.load(this);
    }

    public void onServerTick() {}

    public void onWorldUnload() {
        regionStorage.save(this);
    }

    public OpResult tryCreate(Region candidate, ServerPlayer serverPlayer) {
        NotificationType type = validator.validateCreation(candidate, serverPlayer);
        if (type != NotificationType.REGION_CREATED) {
            return toFailure(type, candidate, candidate.id());
        }
        regions.put(candidate.id(), candidate);
        spatialIndex.index(candidate);
        regionStorage.save(this);
        return OpResult.success(type);
    }

    public OpResult tryUpdate(Region oldRegion, Region candidate, @Nullable ServerPlayer serverPlayer) {
        NotificationType type = validator.validateUpdate(oldRegion, candidate, serverPlayer);
        if (type != NotificationType.REGION_UPDATED) {
            return toFailure(type, candidate, oldRegion.id());
        } else if (!oldRegion.id().equals(candidate.id())) {
            return OpResult.failure(NotificationType.REGION_ID_CONFLICT, List.of());
        }
        spatialIndex.unindex(oldRegion);
        regions.put(candidate.id(), candidate);
        spatialIndex.index(candidate);
        regionStorage.save(this);

        boolean nameChanged = !oldRegion.name().equals(candidate.name());
        boolean colorChanged = !oldRegion.color().equals(candidate.color());
        if (nameChanged || colorChanged) {
            level.players()
                    .stream()
                    .filter(p -> candidate.bounds().isInside(p.blockPosition()))
                    .forEach(p -> PlayerRegionChangeEvent.CHANGED.invoker()
                            .onRegionChanged(p, oldRegion, candidate));
        }
        return OpResult.success(NotificationType.REGION_UPDATED);
    }

    public OpResult tryDelete(UUID regionId, @Nullable ServerPlayer serverPlayer) {
        Region region = regions.get(regionId);
        if (region == null) {
            return OpResult.failure(NotificationType.REGION_NOT_FOUND, List.of());
        }
        if (serverPlayer != null) {
            NotificationType type = validator.validateDeletion(region, serverPlayer);
            if (type != NotificationType.REGION_DELETED) {
                return OpResult.failure(type, List.of());
            }
        }
        regions.remove(regionId);
        spatialIndex.unindex(region);
        regionStorage.save(this);
        return OpResult.success(NotificationType.REGION_DELETED);
    }

    public void loadRegion(Region region) {
        regions.put(region.id(), region);
        spatialIndex.index(region);
    }

    public Optional<Region> getAt(BlockPos pos) {
        long chunkKey = ChunkPos.pack(pos);
        Set<UUID> ids = spatialIndex.getRegionIdsInChunk(chunkKey);
        if (ids.isEmpty()) return Optional.empty();

        for (UUID id : ids) {
            Region region = regions.get(id);
            if (region != null && region.bounds().isInside(pos)) {
                return Optional.of(region);
            }
        }
        return Optional.empty();
    }

    public Optional<Region> getById(UUID id) {
        return Optional.ofNullable(regions.get(id));
    }

    public boolean isMixedChunk(long chunkKey) {
        return spatialIndex.isMixedChunk(chunkKey);
    }

    public Collection<Region> getAll() {
        return Collections.unmodifiableCollection(regions.values());
    }

    public int countOwnedBy(UUID playerUuid) {
        int count = 0;
        for (Region region : regions.values()) {
            if (region.isOwner(playerUuid)) {
                count++;
            }
        }
        return count;
    }

    public int countOwnedByOnServer(UUID playerUuid) {
        int total = 0;
        for (ServerLevel other : level.getServer().getAllLevels()) {
            total += WorldRegistry.get(other).getRegionManager().countOwnedBy(playerUuid);
        }
        return total;
    }

    public boolean intersectsExisting(Region candidate) {
        return regions.values().stream()
                .anyMatch(existing -> existing.bounds().intersects(candidate.bounds()));
    }

    public boolean intersectsExistingExcept(Region candidate, UUID ignoreId) {
        return regions.values().stream()
                .filter(existing -> !existing.id().equals(ignoreId))
                .anyMatch(existing -> existing.bounds().intersects(candidate.bounds()));
    }

    public List<BoundingBox> computeConflictBounds(BoundingBox candidateBounds, UUID ignoreRegionId) {
        return spatialIndex.computeConflictBounds(candidateBounds, ignoreRegionId, regions);
    }

    public RegionNbtPersistence getRegionStorage() {
        return regionStorage;
    }

    private OpResult toFailure(NotificationType type, Region candidate, UUID ignoreId) {
        if (type != NotificationType.REGION_TERRITORY_CONFLICT) {
            return OpResult.failure(type, List.of());
        }
        BoundingBox bounds = candidate != null ? candidate.bounds() : null;
        return OpResult.failure(type, computeConflictBounds(bounds, ignoreId));
    }
}