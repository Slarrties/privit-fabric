package dev.slarrties.privit.server.tracking.redstone;

import dev.slarrties.privit.server.tracking.protection.RedstoneOriginTracker;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.RedStoneWireBlock;

import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class RedstoneTraceService {

    private record TraceNode(BlockPos pos, int power) {}

    private record TraceCacheEntry(UUID playerId, long expirationTick) {
        boolean isValid(long current) {
            return current < expirationTick;
        }
    }

    private final ServerLevel level;
    private final RedstoneOriginTracker redstoneTracker;
    private final ConcurrentHashMap<Long, TraceCacheEntry> traceCache = new ConcurrentHashMap<>();
    private static final int CACHE_TICKS = 20;

    public RedstoneTraceService(RedstoneOriginTracker redstoneTracker, ServerLevel level) {
        this.redstoneTracker = redstoneTracker;
        this.level = level;
    }

    @Nullable
    public UUID getResponsiblePlayer(BlockPos actionPos) {
        if (actionPos == null) return null;
        long key = actionPos.asLong();
        long currentTick = level.getGameTime();
        TraceCacheEntry cached = traceCache.get(key);
        if (cached != null && cached.isValid(currentTick)) {
            return cached.playerId;
        }
        UUID direct = redstoneTracker.getResponsible(actionPos);
        if (direct != null) {
            cacheResult(key, direct, currentTick);
            return direct;
        }
        UUID responsible = trace(actionPos);
        cacheResult(key, responsible, currentTick);
        return responsible;
    }

    private void cacheResult(long posKey, @Nullable UUID playerId, long currentTick) {
        if (playerId != null) {
            traceCache.put(posKey, new TraceCacheEntry(playerId, currentTick + CACHE_TICKS));
        } else {
            traceCache.remove(posKey);
        }
    }

    @Nullable
    private UUID trace(BlockPos startPos) {
        Set<Long> visited = new HashSet<>();
        Queue<TraceNode> queue = new ArrayDeque<>();
        queue.add(new TraceNode(startPos, 20));
        visited.add(startPos.asLong());
        while (!queue.isEmpty()) {
            TraceNode node = queue.poll();
            BlockPos pos = node.pos;
            BlockState state = level.getBlockState(pos);
            if (RedstoneSourceRegistry.isSource(state, level, pos)) {
                UUID owner = redstoneTracker.getResponsible(pos);
                if (owner != null) {
                    return owner;
                }
            }
            if (node.power <= 0) continue;
            for (Direction dir : Direction.values()) {
                BlockPos neighbor = pos.relative(dir);
                long nKey = neighbor.asLong();
                if (visited.contains(nKey)) continue;
                BlockState nState = level.getBlockState(neighbor);
                if (RedstoneSourceRegistry.isSource(nState, level, neighbor)) {
                    visited.add(nKey);
                    queue.add(new TraceNode(neighbor, 20));
                    continue;
                }
                if (nState.getBlock() instanceof RedStoneWireBlock) {
                    int wirePower = nState.getValue(RedStoneWireBlock.POWER);
                    if (wirePower > 0) {
                        visited.add(nKey);
                        queue.add(new TraceNode(neighbor, Math.min(node.power - 1, wirePower)));
                    }
                    continue;
                }
                int receivedPower = level.getBestNeighborSignal(neighbor);
                if (receivedPower > 0) {
                    visited.add(nKey);
                    queue.add(new TraceNode(neighbor, Math.min(node.power - 1, receivedPower)));
                }
            }
        }
        return null;
    }

    public void clearCache() {
        traceCache.clear();
    }
}