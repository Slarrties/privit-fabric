package dev.slarrties.privit.server.region.gui;

import dev.slarrties.privit.PrivitMod;
import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.gui.state.RegionGuiState;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.common.network.payload.c2s.RegionGuiUpdateC2SPacket;
import dev.slarrties.privit.common.network.payload.s2c.RegionGuiCloseS2CPacket;
import dev.slarrties.privit.common.network.payload.s2c.RegionGuiUpdateS2CPacket;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.Region;
import dev.slarrties.privit.server.region.RegionManager;

import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import java.util.Set;
import java.util.UUID;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class RegionGuiSession {

    public sealed interface DeltaResult {
        record Applied() implements DeltaResult {}
        record Locked(String editorName, RegionGuiState state) implements DeltaResult {}
        record Denied(NotificationType type) implements DeltaResult {}
    }

    public sealed interface CancelResult {
        record Restored(RegionGuiState state) implements CancelResult {}
        record NoRegionToRevert() implements CancelResult {}
        record Denied(NotificationType type) implements CancelResult {}
    }

    private record EditorLock(UUID editorId, String editorName, long unlockTimeMs) {}

    private static final long LOCK_DURATION_MS = 5000L;
    private static final long CONFLICT_RECALC_COOLDOWN_MS = 160L;

    private final UUID regionId;
    private final ServerWorld world;
    private final RegionGuiState state;
    private final RegionManager regionManager;
    private final Set<ServerPlayerEntity> viewers = ConcurrentHashMap.newKeySet();
    private EditorLock lock;
    private long lastConflictCalc;
    BlockPos pendingTablePos;

    RegionGuiSession(
            UUID regionId,
            ServerWorld world,
            RegionGuiState state,
            RegionManager regionManager,
            BlockPos pendingTablePos
    ) {
        this.regionId = regionId;
        this.world = world;
        this.state = state;
        this.regionManager = regionManager;
        this.pendingTablePos = pendingTablePos;
    }

    public UUID regionId() {
        return regionId;
    }

    public RegionGuiState state() {
        return state;
    }

    public boolean hasViewers() {
        return !viewers.isEmpty();
    }

    public void addViewer(ServerPlayerEntity player) {
        viewers.add(player);
    }

    public void setPivotIfCreated(BlockPos tablePos) {
        if (state.isCreated()) {
            state.setPivotPos(tablePos);
        }
    }

    public void clearLock() {
        lock = null;
    }

    public void expireLock(long now) {
        if (lock != null && now >= lock.unlockTimeMs()) {
            lock = null;
        }
    }

    public boolean isOwner(UUID playerUuid) {
        return state.getGroups().findByName("owner")
                .map(g -> g.getMembers().contains(playerUuid))
                .orElse(false);
    }

    public BlockBox clamp(BlockBox box, int minY, int maxY) {
        int y1 = Math.clamp(box.getMinY(), minY, maxY);
        int y2 = Math.clamp(box.getMaxY(), minY, maxY);

        if (y1 > y2) {
            int t = y1; y1 = y2; y2 = t;
        }

        return new BlockBox(
                box.getMinX(), y1, box.getMinZ(),
                box.getMaxX(), y2, box.getMaxZ()
        );
    }

    public DeltaResult applyDelta(ServerPlayerEntity player, RegionGuiUpdateC2SPacket delta) {
        long now = System.currentTimeMillis();
        if (lock != null
                && now < lock.unlockTimeMs()
                && !lock.editorId().equals(player.getUuid())) {
            return new DeltaResult.Locked(lock.editorName(), state);
        }

        lock = new EditorLock(
                player.getUuid(),
                player.getName().getString(),
                now + LOCK_DURATION_MS
        );

        if (!isOwner(player.getUuid())) {
            return new DeltaResult.Denied(NotificationType.DENY_MANAGE);
        }

        applyDeltaToState(delta);

        List<BlockBox> conflicts = state.getConflictBounds();
        if (lastConflictCalc == 0 || now - lastConflictCalc >= CONFLICT_RECALC_COOLDOWN_MS) {
            if (state.getDraftBounds() != null) {
                conflicts = regionManager.computeConflictBounds(state.getDraftBounds(), regionId);
            }
            state.setConflictBounds(conflicts);
            lastConflictCalc = now;
        }

        RegionGuiMapping.recalculateChanged(state, regionManager.getById(regionId).orElse(null));

        broadcast(new RegionGuiUpdateS2CPacket(
                regionId,
                state.isChanged(),
                player.getName().getString(),
                delta.name(),
                delta.realBounds(),
                Optional.of(state.getDraftBounds()),
                Optional.of(state.getConflictBounds()),
                delta.pivotPos(),
                delta.color(),
                delta.groups(),
                delta.isCreated(),
                Optional.of(state.isAreaLimitExceeded())
        ));

        Color gridColor = state.isCreated()
                ? regionManager.getById(regionId).map(Region::color).orElse(state.getColor())
                : state.getColor();

        WorldRegistry.get(world).getGridSubscriptions().publish(
                regionId,
                gridColor,
                state.getRealBounds(),
                state.getDraftBounds(),
                state.getConflictBounds(),
                world.getPlayers()
        );

        return new DeltaResult.Applied();
    }

    public CancelResult cancel(ServerPlayerEntity player) {
        Optional<Region> realOpt = regionManager.getById(regionId);
        if (realOpt.isEmpty()) {
            return new CancelResult.NoRegionToRevert();
        }

        Region real = realOpt.get();
        if (!real.isOwner(player.getUuid())) {
            return new CancelResult.Denied(NotificationType.DENY_MANAGE);
        }

        RegionGuiMapping.fillFromRegion(state, real);
        broadcast(snapshot("", false));

        WorldRegistry.get(world).getGridSubscriptions().publish(
                regionId,
                state.getColor(),
                state.getRealBounds(),
                state.getDraftBounds(),
                state.getConflictBounds(),
                world.getPlayers()
        );

        return new CancelResult.Restored(state);
    }

    public void replaceCommitted(Region region) {
        RegionGuiMapping.fillFromRegion(state, region);
        lock = null;
        pendingTablePos = null;
        broadcast(snapshot("", false));

        WorldRegistry.get(world).getGridSubscriptions().publish(
                regionId,
                state.getColor(),
                state.getRealBounds(),
                state.getDraftBounds(),
                state.getConflictBounds(),
                world.getPlayers()
        );
    }

    public void notifyCommitRejected(RegionManager.OpResult result, ServerPlayerEntity actor) {
        if (result.type() != NotificationType.REGION_TERRITORY_CONFLICT) {
            return;
        }
        state.setConflictBounds(result.conflicts());
        broadcast(snapshot(actor.getName().getString(), state.isChanged()));
    }

    public void sendClose() {
        CustomPayload close = new RegionGuiCloseS2CPacket(regionId);
        for (ServerPlayerEntity viewer : Set.copyOf(viewers)) {
            ServerPlayNetworking.send(viewer, close);
        }
        viewers.clear();
    }

    private RegionGuiUpdateS2CPacket snapshot(String editorName, boolean changed) {
        return new RegionGuiUpdateS2CPacket(
                regionId,
                changed,
                editorName,
                Optional.of(state.getName()),
                Optional.ofNullable(state.getRealBounds()),
                Optional.ofNullable(state.getDraftBounds()),
                Optional.of(state.getConflictBounds()),
                Optional.of(state.getPivotPos()),
                Optional.of(state.getColor()),
                Optional.of(state.getGroups()),
                Optional.of(state.isCreated()),
                Optional.of(state.isAreaLimitExceeded())
        );
    }

    private void broadcast(CustomPayload packet) {
        for (ServerPlayerEntity viewer : viewers) {
            ServerPlayNetworking.send(viewer, packet);
        }
    }

    private void applyDeltaToState(RegionGuiUpdateC2SPacket delta) {
        delta.name().ifPresent(state::setName);
        delta.draftBounds().ifPresent(box ->
                state.setDraftBounds(this.clamp(box, world.getBottomY(), world.getTopY() - 1))
        );
        delta.pivotPos().ifPresent(state::setPivotPos);
        delta.color().ifPresent(state::setColor);
        delta.groups().ifPresent(newGroups -> {
            try {
                state.applyGroups(newGroups);
            } catch (IllegalArgumentException e) {
                if (e.getMessage() != null && e.getMessage().contains("Too many groups")) {
                    PrivitMod.LOGGER.warn("[RegionGuiSession] group limit exceeded for {}", state.getId());
                }
                throw e;
            }
        });
    }
}