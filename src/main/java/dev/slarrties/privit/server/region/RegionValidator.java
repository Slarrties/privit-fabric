package dev.slarrties.privit.server.region;

import dev.slarrties.privit.common.config.ConfigManager;
import dev.slarrties.privit.common.notification.NotificationType;

import net.minecraft.util.math.BlockBox;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Set;
import java.util.UUID;

public final class RegionValidator {

    private final RegionManager manager;

    public RegionValidator(RegionManager manager) {
        this.manager = manager;
    }

    public NotificationType validateCreation(Region candidate, ServerPlayerEntity player) {
        if (!candidate.isOwner(player.getUuid())) {
            return NotificationType.DENY_MANAGE;
        }

        if (manager.intersectsExisting(candidate)) {
            return NotificationType.REGION_TERRITORY_CONFLICT;
        }

        if (isTooBig(candidate)) {
            return NotificationType.REGION_TOO_BIG;
        }

        for (UUID ownerId : ownerIds(candidate)) {
            if (isRegionLimitExceeded(ownerId)) {
                return NotificationType.REGIONS_LIMIT_REACHED;
            }
        }

        return NotificationType.REGION_CREATED;
    }

    public NotificationType validateUpdate(Region oldRegion, Region newRegion, ServerPlayerEntity player) {
        if (!oldRegion.id().equals(newRegion.id())) {
            return NotificationType.REGION_ID_CONFLICT;
        }

        if (!oldRegion.isOwner(player.getUuid())) {
            return NotificationType.DENY_MANAGE;
        }

        if (manager.intersectsExistingExcept(newRegion, oldRegion.id())) {
            return NotificationType.REGION_TERRITORY_CONFLICT;
        }

        if (isTooBig(newRegion)) {
            return NotificationType.REGION_TOO_BIG;
        }

        Set<UUID> oldOwners = ownerIds(oldRegion);
        Set<UUID> newOwners = ownerIds(newRegion);

        for (UUID id : newOwners) {
            if (!oldOwners.contains(id) && isRegionLimitExceeded(id)) {
                return NotificationType.REGIONS_LIMIT_REACHED;
            }
        }

        return NotificationType.REGION_UPDATED;
    }

    public NotificationType validateDeletion(Region region, ServerPlayerEntity player) {
        if (region == null) {
            return NotificationType.REGION_DELETION_FAILED;
        }

        if (!region.isOwner(player.getUuid())) {
            return NotificationType.DENY_MANAGE;
        }

        return NotificationType.REGION_DELETED;
    }

    private boolean isTooBig(Region region) {
        int maxArea = ConfigManager.get().regionLimits.maxArea;
        if (maxArea <= 0) return false;
        return calculateVolume(region.bounds()) > maxArea;
    }

    private boolean isRegionLimitExceeded(UUID playerUuid) {
        int max = ConfigManager.get().regionLimits.maxRegionsPerPlayer;
        if (max <= 0) return false;
        return manager.countOwnedByOnServer(playerUuid) >= max;
    }

    private long calculateVolume(BlockBox bounds) {
        long sizeX = bounds.getMaxX() - bounds.getMinX() + 1L;
        long sizeY = bounds.getMaxY() - bounds.getMinY() + 1L;
        long sizeZ = bounds.getMaxZ() - bounds.getMinZ() + 1L;
        return sizeX * sizeY * sizeZ;
    }

    private Set<UUID> ownerIds(Region region) {
        return region.groups().findByName("owner")
                .map(g -> Set.copyOf(g.getMembers()))
                .orElse(Set.of());
    }
}