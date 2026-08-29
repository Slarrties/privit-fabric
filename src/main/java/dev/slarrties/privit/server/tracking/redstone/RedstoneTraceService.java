package dev.slarrties.privit.server.tracking.redstone;

import dev.slarrties.privit.server.tracking.protection.RedstoneOriginTracker;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.block.BlockState;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RedstoneTraceService {

    private final ServerWorld world;
    private final RedstoneOriginTracker redstoneTracker;
    private final ConcurrentHashMap<Long, TraceCacheEntry> traceCache = new ConcurrentHashMap<>();

    private static final int CACHE_TICKS = 20;
    // private static final int MAX_TRACE_DEPTH = 25;

    public RedstoneTraceService(RedstoneOriginTracker redstoneTracker, ServerWorld world) {
        this.redstoneTracker = redstoneTracker;
        this.world = world;
    }

    @Nullable
    public UUID getResponsiblePlayer(BlockPos actionPos) {
        if (actionPos == null) return null;

        long key = actionPos.asLong();
        long currentTick = world.getTime();

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
            BlockState state = world.getBlockState(pos);

            if (RedstoneSourceRegistry.isSource(state, world, pos)) {
                UUID owner = redstoneTracker.getResponsible(pos);
                if (owner != null) {
                    return owner;
                }
            }

            if (node.power <= 0) continue;

            for (Direction dir : Direction.values()) {
                BlockPos neighbor = pos.offset(dir);
                long nKey = neighbor.asLong();
                if (visited.contains(nKey)) continue;

                BlockState nState = world.getBlockState(neighbor);

                if (RedstoneSourceRegistry.isSource(nState, world, neighbor)) {
                    visited.add(nKey);
                    queue.add(new TraceNode(neighbor, 20));
                    continue;
                }

                if (nState.getBlock() instanceof RedstoneWireBlock) {
                    int wirePower = nState.get(RedstoneWireBlock.POWER);
                    if (wirePower > 0) {
                        visited.add(nKey);
                        queue.add(new TraceNode(neighbor, Math.min(node.power - 1, wirePower)));
                    }
                    continue;
                }

                int receivedPower = world.getReceivedRedstonePower(neighbor);
                if (receivedPower > 0) {
                    visited.add(nKey);
                    queue.add(new TraceNode(neighbor, Math.min(node.power - 1, receivedPower)));
                }
            }
        }

        return null;
    }

    private record TraceNode(BlockPos pos, int power) {}

    private record TraceCacheEntry(UUID playerId, long expirationTick) {
        boolean isValid(long current) {
            return current < expirationTick;
        }
    }

    public void clearCache() {
        traceCache.clear();
    }
}