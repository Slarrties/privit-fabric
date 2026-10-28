package dev.slarrties.privit.server.tracking.protection;

import dev.slarrties.privit.PrivitMod;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.tracking.origin.OriginTracker;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.boss.wither.WitherBoss;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// TODO: make it a part of influenced tracker
public final class WitherPlacerTracker implements OriginTracker {

    private static final Map<BlockPos, UUID> placerCache = new ConcurrentHashMap<>();

    public WitherPlacerTracker() {}

    public static void recordPlacer(BlockPos skullPos, ServerPlayer player) {
        if (skullPos == null || player == null || player.level().isClientSide()) return;

        placerCache.put(skullPos, player.getUUID());
    }

    public static void applyToWither(WitherBoss wither) {
        if (wither == null) return;

        if (wither.level() instanceof ServerLevel serverLevel) {
            BlockPos witherPos = wither.getOnPos();
            UUID foundUuid = null;

            for (Map.Entry<BlockPos, UUID> entry : placerCache.entrySet()) {
                BlockPos skullPos = entry.getKey();

                if (skullPos.distSqr(witherPos) <= 64) {
                    foundUuid = entry.getValue();
                    placerCache.remove(skullPos);
                    break;
                }
            }

            if (foundUuid != null) {
                ServerPlayer player = wither.level().getServer().getPlayerList().getPlayer(foundUuid); // TODO: player can be null

                if (player != null) {
                    WorldRegistry.get(serverLevel)
                            .getTrackerManager()
                            .getExplosionOriginTracker()
                            .record(wither, player.getUUID());
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