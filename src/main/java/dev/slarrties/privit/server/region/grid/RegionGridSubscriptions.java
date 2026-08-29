package dev.slarrties.privit.server.region.grid;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.network.payload.s2c.RegionGridStateS2CPacket;
import dev.slarrties.privit.server.region.Region;

import net.minecraft.util.math.BlockBox;
import net.minecraft.server.network.ServerPlayerEntity;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import java.util.Set;
import java.util.Map;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RegionGridSubscriptions {

    private final Map<UUID, Set<UUID>> subscribers = new ConcurrentHashMap<>();

    public void subscribe(UUID regionId, ServerPlayerEntity player) {
        subscribers
                .computeIfAbsent(regionId, id -> ConcurrentHashMap.newKeySet())
                .add(player.getUuid());
    }

    public void unsubscribe(UUID regionId, ServerPlayerEntity player) {
        Set<UUID> set = subscribers.get(regionId);
        if (set == null) return;
        set.remove(player.getUuid());
        if (set.isEmpty()) subscribers.remove(regionId);
    }

    public void unsubscribeAll(UUID regionId) {
        subscribers.remove(regionId);
    }

    public void onPlayerLeave(UUID playerId) {
        subscribers.values().forEach(set -> set.remove(playerId));
        subscribers.entrySet().removeIf(e -> e.getValue().isEmpty());
    }

    public void publish(Region region, Iterable<ServerPlayerEntity> players) {
        publish(
                region.id(),
                region.color(),
                region.bounds(),
                region.bounds(),
                List.of(),
                players
        );
    }

    public void publish(
            UUID regionId,
            Color color,
            BlockBox realBounds,
            BlockBox draftBounds,
            List<BlockBox> conflicts,
            Iterable<ServerPlayerEntity> players
    ) {
        Set<UUID> ids = subscribers.get(regionId);
        if (ids == null || ids.isEmpty()) return;

        RegionGridStateS2CPacket packet = RegionGridStateS2CPacket.show(
                regionId, color, realBounds, draftBounds, conflicts);

        for (ServerPlayerEntity player : players) {
            if (ids.contains(player.getUuid())) {
                ServerPlayNetworking.send(player, packet);
            }
        }
    }

    public void hide(UUID regionId, Iterable<ServerPlayerEntity> players) {
        Set<UUID> ids = subscribers.remove(regionId);
        if (ids == null || ids.isEmpty()) return;

        RegionGridStateS2CPacket packet = RegionGridStateS2CPacket.hide(regionId);
        for (ServerPlayerEntity player : players) {
            if (ids.contains(player.getUuid())) {
                ServerPlayNetworking.send(player, packet);
            }
        }
    }

    public void clear() {
        subscribers.clear();
    }
}