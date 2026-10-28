package dev.slarrties.privit.server.tracking;

import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.Region;
import dev.slarrties.privit.server.region.event.PlayerRegionChangeEvent;
import dev.slarrties.privit.server.region.protection.PlayerPermissionCache;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import java.util.Map;
import java.util.UUID;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerRegionPresenceTracker {
    private static final Map<UUID, Region> lastKnownRegion = new ConcurrentHashMap<>();

    private PlayerRegionPresenceTracker() {}

    public static void init() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            PlayerPermissionCache.invalidatePlayer(player);
            server.submit(() -> checkPlayer(player));
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID uuid = handler.getPlayer().getUUID();
            lastKnownRegion.remove(uuid);
            PlayerPermissionCache.invalidatePlayer(handler.getPlayer());
        });

        PlayerRegionChangeEvent.CHANGED.register(PlayerPermissionCache::onRegionChanged);

        ServerTickEvents.END_LEVEL_TICK.register(level -> {
            if (level.getGameTime() % 5 != 0) return;
            for (ServerPlayer player : level.players()) {
                checkPlayer(player);
            }
        });
    }

    public static void refreshPlayersHud(ServerLevel level, BoundingBox bounds) {
        if (level == null || bounds == null) return;

        for (ServerPlayer player : level.players()) {
            if (bounds.isInside(player.blockPosition())) {
                checkPlayer(player);
            }
        }
    }

    private static void checkPlayer(ServerPlayer player) {
        UUID uuid = player.getUUID();
        BlockPos pos = player.blockPosition();
        Region previous = lastKnownRegion.get(uuid);
        Region current = WorldRegistry.get(player.level())
                .getRegionManager()
                .getAt(pos)
                .orElse(null);

        if (Objects.equals(previous, current)) return;
        if (current != null) {
            lastKnownRegion.put(uuid, current);
        } else {
            lastKnownRegion.remove(uuid);
        }

        PlayerRegionChangeEvent.CHANGED.invoker().onRegionChanged(player, previous, current);
    }

    public static Region getCurrentRegion(ServerPlayer player) {
        return lastKnownRegion.get(player.getUUID());
    }

    public static Region getCurrentRegionById(UUID playerUuid) {
        return lastKnownRegion.get(playerUuid);
    }
}