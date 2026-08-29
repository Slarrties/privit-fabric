package dev.slarrties.privit.server.tracking;

import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.Region;
import dev.slarrties.privit.server.region.event.PlayerRegionChangeEvent;
import dev.slarrties.privit.server.region.protection.PlayerPermissionCache;

import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;
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
            ServerPlayerEntity player = handler.getPlayer();
            PlayerPermissionCache.invalidatePlayer(player);
            server.submit(() -> checkPlayer(player));
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID uuid = handler.getPlayer().getUuid();
            lastKnownRegion.remove(uuid);
            PlayerPermissionCache.invalidatePlayer(handler.getPlayer());
        });

        PlayerRegionChangeEvent.CHANGED.register(PlayerPermissionCache::onRegionChanged);

        ServerTickEvents.END_WORLD_TICK.register(world -> {
            if (world.getTime() % 5 != 0) return;

            for (ServerPlayerEntity player : world.getPlayers()) {
                checkPlayer(player);
            }
        });
    }

    public static void refreshPlayersHud(ServerWorld world, BlockBox bounds) {
        if (world == null || bounds == null) return;
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (bounds.contains(player.getBlockPos())) {
                checkPlayer(player);
            }
        }
    }

    private static void checkPlayer(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        BlockPos pos = player.getBlockPos();
        Region current = WorldRegistry.get(player.getServerWorld())
                .getRegionManager()
                .getAt(pos)
                .orElse(null);
        Region previous = lastKnownRegion.get(uuid);

        if (Objects.equals(previous, current)) return;
        if (current != null) {
            lastKnownRegion.put(uuid, current);
        } else {
            lastKnownRegion.remove(uuid);
        }

        PlayerRegionChangeEvent.CHANGED.invoker().onRegionChanged(player, previous, current);
    }

    public static Region getCurrentRegion(ServerPlayerEntity player) {
        return lastKnownRegion.get(player.getUuid());
    }

    public static Region getCurrentRegionById(UUID playerUuid) {
        return lastKnownRegion.get(playerUuid);
    }
}