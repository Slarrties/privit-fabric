package dev.slarrties.privit.server.util;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.common.network.payload.s2c.HudNotificationS2CPacket;

import net.minecraft.server.network.ServerPlayerEntity;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerNotification {

    private static final long DEFAULT_COOLDOWN_MS = 1800L;
    private static final Map<ServerPlayerEntity, Map<NotificationType, Long>> lastSent = new WeakHashMap<>();
    private static final Map<NotificationType, Long> TYPE_COOLDOWNS = Map.of(
            NotificationType.DENY_ITEM_DROP, 1200L,
            NotificationType.DENY_ITEM_PICKUP, 1200L
    );

    private PlayerNotification() {}

    public static boolean trySend(ServerPlayerEntity player, NotificationType type) {
        return trySend(player, type, Color.RED);
    }

    public static boolean trySend(ServerPlayerEntity player, NotificationType type, Color color) {
        if (player == null) return false;

        long now = System.currentTimeMillis();
        long cooldown = TYPE_COOLDOWNS.getOrDefault(type, DEFAULT_COOLDOWN_MS);

        Map<NotificationType, Long> playerMap = lastSent.computeIfAbsent(player, k -> new ConcurrentHashMap<>());
        Long lastTime = playerMap.get(type);

        if (lastTime != null && now - lastTime < cooldown) {
            return false;
        }

        playerMap.put(type, now);
        ServerPlayNetworking.send(player, new HudNotificationS2CPacket(type, color));
        return true;
    }

    public static void clearForPlayer(ServerPlayerEntity player) {
        if (player != null) {
            lastSent.remove(player);
        }
    }
}