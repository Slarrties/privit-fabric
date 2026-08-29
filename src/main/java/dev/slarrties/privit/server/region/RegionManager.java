package dev.slarrties.privit.server.region;

import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.nbt.RegionNbtPersistence;
import dev.slarrties.privit.server.region.event.PlayerRegionChangeEvent;

import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.*;
import org.jetbrains.annotations.Nullable;

public final class RegionManager {

    // switch to the sealed interface?
    public record OpResult(NotificationType type, List<BlockBox> conflicts) {

        public boolean isSuccess() {
            return type == NotificationType.REGION_CREATED
                    || type == NotificationType.REGION_UPDATED
                    || type == NotificationType.REGION_DELETED;
        }

        public static OpResult success(NotificationType type) {
            return new OpResult(type, List.of());
        }

        public static OpResult failure(NotificationType type, List<BlockBox> conflicts) {
            return new OpResult(type, conflicts == null ? List.of() : List.copyOf(conflicts));
        }
    }

    private final ServerWorld world;
    private final RegionNbtPersistence regionStorage;
    private final Map<UUID, Region> regions = new HashMap<>();
    private final RegionSpatialIndex spatialIndex = new RegionSpatialIndex();
    private final RegionValidator validator;

    public RegionManager(ServerWorld world) {
        this.world = world;
        this.regionStorage = new RegionNbtPersistence(world);
        this.validator = new RegionValidator(this);
        regionStorage.load(this);
    }

    public void onServerTick() {}

    public void onWorldUnload() {
        regionStorage.save(this);
    }

    public OpResult tryCreate(Region candidate, ServerPlayerEntity actor) {
        NotificationType type = validator.validateCreation(candidate, actor);
        if (type != NotificationType.REGION_CREATED) {
            return toFailure(type, candidate, candidate.id());
        }

        regions.put(candidate.id(), candidate);
        spatialIndex.index(candidate);
        regionStorage.save(this);
        return OpResult.success(type);
    }

    public OpResult tryUpdate(Region oldRegion, Region candidate, @Nullable ServerPlayerEntity actor) {
        NotificationType type = validator.validateUpdate(oldRegion, candidate, actor);

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
            world.getPlayers()
                    .stream()
                    .filter(p -> candidate.bounds().contains(p.getBlockPos()))
                    .forEach(p -> PlayerRegionChangeEvent.CHANGED.invoker()
                            .onRegionChanged(p, oldRegion, candidate));
        }

        return OpResult.success(NotificationType.REGION_UPDATED);
    }

    public OpResult tryDelete(UUID regionId, @Nullable ServerPlayerEntity actor) {
        Region region = regions.get(regionId);
        if (region == null) {
            return OpResult.failure(NotificationType.REGION_NOT_FOUND, List.of());
        }

        if (actor != null) {
            NotificationType type = validator.validateDeletion(region, actor);
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
        Set<UUID> ids = spatialIndex.getRegionIdsInChunk(new ChunkPos(pos).toLong());
        if (ids.isEmpty()) return Optional.empty();

        for (UUID id : ids) {
            Region region = regions.get(id);
            if (region != null && region.bounds().contains(pos)) {
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

    // violation of the single responsibility
    public int countOwnedByOnServer(UUID playerUuid) {
        int total = 0;

        for (ServerWorld other : world.getServer().getWorlds()) {
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

    public List<BlockBox> computeConflictBounds(BlockBox candidateBounds, UUID ignoreRegionId) {
        return spatialIndex.computeConflictBounds(candidateBounds, ignoreRegionId, regions);
    }

    public RegionNbtPersistence getRegionStorage() {
        return regionStorage;
    }

    private OpResult toFailure(NotificationType type, Region candidate, UUID ignoreId) {
        if (type != NotificationType.REGION_TERRITORY_CONFLICT) {
            return OpResult.failure(type, List.of());
        }

        BlockBox bounds = candidate != null ? candidate.bounds() : null;
        return OpResult.failure(type, computeConflictBounds(bounds, ignoreId));
    }
}