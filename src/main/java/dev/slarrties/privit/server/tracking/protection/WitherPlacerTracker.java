package dev.slarrties.privit.server.tracking.protection;

import dev.slarrties.privit.PrivitMod;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.tracking.origin.OriginTracker;

import net.minecraft.util.math.BlockPos;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// TODO: make it a part of influenced tracker
public final class WitherPlacerTracker implements OriginTracker {

    private static final Map<BlockPos, UUID> placerCache = new ConcurrentHashMap<>();

    public WitherPlacerTracker() {}

    public static void recordPlacer(BlockPos skullPos, ServerPlayerEntity player) {
        if (skullPos == null || player == null || player.getWorld().isClient) return;

        placerCache.put(skullPos, player.getUuid());
    }

    public static void applyToWither(WitherEntity wither) {
        if (wither == null) return;

        if (wither.getWorld() instanceof ServerWorld serverWorld) {
            BlockPos witherPos = wither.getBlockPos();
            UUID foundUuid = null;

            for (Map.Entry<BlockPos, UUID> entry : placerCache.entrySet()) {
                BlockPos skullPos = entry.getKey();

                if (skullPos.getSquaredDistance(witherPos) <= 64) {
                    foundUuid = entry.getValue();
                    placerCache.remove(skullPos);
                    break;
                }
            }

            if (foundUuid != null) {
                ServerPlayerEntity player = wither.getWorld().getServer().getPlayerManager().getPlayer(foundUuid); // TODO: player can be null

                if (player != null) {
                    WorldRegistry.get(serverWorld)
                            .getTrackerManager()
                            .getExplosionOriginTracker()
                            .record(wither, player.getUuid());
                }
            }
        }
    }

    @Override
    public void clearAll() {
        placerCache.clear();
    }

    @Override
    public void onWorldUnload() {
        clearAll();
    }
}