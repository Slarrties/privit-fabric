package dev.slarrties.privit.server.region.gui;

import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.common.region.gui.state.RegionGuiState;
import dev.slarrties.privit.server.region.Region;
import dev.slarrties.privit.server.region.RegionManager;

import net.minecraft.util.math.BlockPos;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RegionGuiSessions {

    public sealed interface OpenResult {
        record Opened(RegionGuiState state) implements OpenResult {}
        record Denied(NotificationType type) implements OpenResult {}
    }

    private final RegionManager regionManager;
    private final Map<UUID, RegionGuiSession> sessions = new ConcurrentHashMap<>();
    private final Map<BlockPos, UUID> pendingByTable = new ConcurrentHashMap<>();

    public RegionGuiSessions(ServerWorld world, RegionManager regionManager) {
        this.regionManager = regionManager;
    }

    public OpenResult open(ServerPlayerEntity player, BlockPos tablePos) {
        Region existing = regionManager.getAt(tablePos).orElse(null);
        if (existing != null && !existing.isOwner(player.getUuid())) {
            return new OpenResult.Denied(NotificationType.REGION_NOT_ACCEPTED);
        }

        RegionGuiSession session = getOrCreate(tablePos, player, existing);
        session.addViewer(player);
        session.setPivotIfCreated(tablePos);
        return new OpenResult.Opened(session.state());
    }

    public RegionGuiState ensureSession(ServerPlayerEntity player, BlockPos tablePos) {
        Region existing = regionManager.getAt(tablePos).orElse(null);
        RegionGuiSession session = getOrCreate(tablePos, player, existing);
        session.addViewer(player);
        session.setPivotIfCreated(tablePos);
        return session.state();
    }

    public RegionGuiSession find(UUID regionId) {
        return sessions.get(regionId);
    }

    public void close(UUID regionId) {
        RegionGuiSession session = sessions.remove(regionId);
        if (session == null) return;

        if (session.pendingTablePos != null) {
            pendingByTable.remove(session.pendingTablePos, regionId);
        }
        session.sendClose();
    }

    public UUID closePendingAt(BlockPos tablePos) {
        UUID regionId = pendingByTable.remove(tablePos);
        if (regionId == null) return null;
        close(regionId);
        return regionId;
    }

    public void onTick() {
        long now = System.currentTimeMillis();
        sessions.values().forEach(session -> session.expireLock(now));
        sessions.entrySet().removeIf(entry -> {
            RegionGuiSession session = entry.getValue();
            if (session.hasViewers()) return false;
            if (session.pendingTablePos != null) {
                pendingByTable.remove(session.pendingTablePos, entry.getKey());
            }
            return true;
        });
    }

    public void onWorldUnload() {
        sessions.clear();
        pendingByTable.clear();
    }

    private RegionGuiSession getOrCreate(BlockPos tablePos, ServerPlayerEntity player, Region existing) {
        UUID regionId = existing != null
                ? existing.id()
                : pendingByTable.computeIfAbsent(tablePos, pos -> generateUniqueId());

        return sessions.computeIfAbsent(regionId, id -> {
            RegionGuiState state = existing != null
                    ? RegionGuiMapping.fromRegion(existing, tablePos)
                    : RegionGuiState.createNew(
                    id,
                    player.getUuid(),
                    tablePos,
                    player.getName().getString()
            );

            if (state.getDraftBounds() != null) {
                state.setConflictBounds(
                        regionManager.computeConflictBounds(state.getDraftBounds(), id)
                );
            }

            return new RegionGuiSession(
                    id,
                    player.getServerWorld(),
                    state,
                    regionManager,
                    existing == null ? tablePos : null
            );
        });
    }

    private UUID generateUniqueId() {
        UUID id;
        do {
            id = UUID.randomUUID();
        } while (regionManager.getById(id).isPresent() || sessions.containsKey(id));
        return id;
    }
}