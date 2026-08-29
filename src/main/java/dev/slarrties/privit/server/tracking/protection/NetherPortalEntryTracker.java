package dev.slarrties.privit.server.tracking.protection;

import dev.slarrties.privit.server.tracking.origin.OriginTracker;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Map;
import java.util.UUID;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

public final class NetherPortalEntryTracker implements OriginTracker {

    private static final long TTL_MS = 10000;

    private static final Map<UUID, Long> recentEntries = new ConcurrentHashMap<>();

    public static void recordEntry(ServerPlayerEntity player) {
        if (player == null) return;
        recentEntries.put(player.getUuid(), System.currentTimeMillis());
    }

    public static ServerPlayerEntity getRecentPortalUser(ServerWorld world) {
        if (world == null) return null;

        long now = System.currentTimeMillis();
        ServerPlayerEntity found = null;

        Iterator<Map.Entry<UUID, Long>> it = recentEntries.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry<UUID, Long> entry = it.next();
            if (now - entry.getValue() > TTL_MS) {
                it.remove();
                continue;
            }

            ServerPlayerEntity player = world.getServer().getPlayerManager().getPlayer(entry.getKey());
            if (player != null && player.isAlive()) {
                found = player;
                break;
            }
        }

        return found;
    }

    @Override
    public void clearAll() {
        recentEntries.clear();
    }

    @Override
    public void onWorldUnload() {
        clearAll();
    }
}