package dev.slarrties.privit.server.region;

import dev.slarrties.privit.common.config.ConfigManager;
import dev.slarrties.privit.common.notification.NotificationType;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.Set;
import java.util.UUID;

public final class RegionValidator {
    private final RegionManager manager;

    public RegionValidator(RegionManager manager) {
        this.manager = manager;
    }

    public NotificationType validateCreation(Region candidate, ServerPlayer player) {
        if (!candidate.isOwner(player.getUUID())) return NotificationType.DENY_MANAGE;
        if (manager.intersectsExisting(candidate)) return NotificationType.REGION_TERRITORY_CONFLICT;
        if (isTooBig(candidate)) return NotificationType.REGION_TOO_BIG;

        for (UUID ownerId : ownerIds(candidate)) {
            if (isRegionLimitExceeded(ownerId))
                return NotificationType.REGIONS_LIMIT_REACHED;
        }

        return NotificationType.REGION_CREATED;
    }

    public NotificationType validateUpdate(Region oldRegion, Region newRegion, ServerPlayer player) {
        if (!oldRegion.id().equals(newRegion.id())) return NotificationType.REGION_ID_CONFLICT;
        if (!oldRegion.isOwner(player.getUUID())) return NotificationType.DENY_MANAGE;
        if (manager.intersectsExistingExcept(newRegion, oldRegion.id())) return NotificationType.REGION_TERRITORY_CONFLICT;
        if (isTooBig(newRegion)) return NotificationType.REGION_TOO_BIG;

        Set<UUID> oldOwners = ownerIds(oldRegion);
        Set<UUID> newOwners = ownerIds(newRegion);
        for (UUID id : newOwners) {
            if (!oldOwners.contains(id) && isRegionLimitExceeded(id))
                return NotificationType.REGIONS_LIMIT_REACHED;
        }

        return NotificationType.REGION_UPDATED;
    }

    public NotificationType validateDeletion(Region region, ServerPlayer player) {
        if (region == null) return NotificationType.REGION_DELETION_FAILED;
        if (!region.isOwner(player.getUUID())) return NotificationType.DENY_MANAGE;

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

    private long calculateVolume(BoundingBox bounds) {
        long sizeX = bounds.getXSpan();
        long sizeY = bounds.getYSpan();
        long sizeZ = bounds.getZSpan();
        return sizeX * sizeY * sizeZ;
    }

    private Set<UUID> ownerIds(Region region) {
        return region.groups().findByName("owner")
                .map(g -> Set.copyOf(g.getMembers()))
                .orElse(Set.of());
    }
}