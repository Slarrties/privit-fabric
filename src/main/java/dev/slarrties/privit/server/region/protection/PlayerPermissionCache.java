package dev.slarrties.privit.server.region.protection;

import dev.slarrties.privit.server.region.Region;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.tracking.PlayerRegionPresenceTracker;

import net.minecraft.util.math.BlockPos;
import net.minecraft.server.network.ServerPlayerEntity;

import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.EnumMap;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerPermissionCache {

    private static final Map<UUID, Map<Rule, Boolean>> PLAYER_CACHE = new ConcurrentHashMap<>();

    private PlayerPermissionCache() {}

    public static boolean isAllowed(ServerPlayerEntity player, Rule rule, BlockPos pos) {
        if (player.getWorld().isClient) return true;

        UUID uuid = player.getUuid();
        Region currentRegion = PlayerRegionPresenceTracker.getCurrentRegion(player);

        if (currentRegion == null) {
            PLAYER_CACHE.remove(uuid);
            return true;
        }

        Map<Rule, Boolean> playerRules = PLAYER_CACHE.computeIfAbsent(uuid, k -> new EnumMap<>(Rule.class));

        if (!playerRules.containsKey(rule)) {
            boolean allowed = RegionPermissionChecker.isAllowed(player, rule, pos);
            playerRules.put(rule, allowed);
            return allowed;
        }

        return playerRules.get(rule);
    }

    public static void onRegionChanged(ServerPlayerEntity player, @Nullable Region from, @Nullable Region to) {
        PLAYER_CACHE.remove(player.getUuid());
    }

    public static void invalidatePlayer(ServerPlayerEntity player) {
        if (player != null) PLAYER_CACHE.remove(player.getUuid());
    }

    public static void invalidateRegion(Region region) {
        if (region == null) return;

        PLAYER_CACHE.entrySet().removeIf(entry -> {
            Region playerRegion = PlayerRegionPresenceTracker.getCurrentRegionById(entry.getKey());
            return playerRegion != null && playerRegion.id().equals(region.id());
        });
    }

    public static void clear() { PLAYER_CACHE.clear(); }
}