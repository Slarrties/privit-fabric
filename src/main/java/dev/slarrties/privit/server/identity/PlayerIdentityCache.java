package dev.slarrties.privit.server.identity;

import dev.slarrties.privit.PrivitMod;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import java.util.*;
import java.util.stream.Stream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;

public final class PlayerIdentityCache {

    private static MinecraftServer server;
    private static final Map<UUID, String> profileCache = new HashMap<>();
    private static final int DEFAULT_LIMIT = 500;
    private static final int MAX_LIMIT = 1000;

    private PlayerIdentityCache() {}

    public static void init(MinecraftServer serverInstance) {
        server = serverInstance;
        loadProfiles();
        registerEvents();
    }

    private static void loadProfiles() {
        PrivitMod.LOGGER.info("[PlayerIdentityCache] Scanning playerdata directory to build local identity index...");
        scanPlayerData();
    }

    private static void scanPlayerData() {
        Path playerDataDir = server.getWorldPath(LevelResource.PLAYER_DATA_DIR);
        if (!Files.exists(playerDataDir)) {
            PrivitMod.LOGGER.warn("[PlayerIdentityCache] playerdata directory not found — no profiles loaded");
            return;
        }

        try (Stream<Path> stream = Files.list(playerDataDir)) {
            stream.filter(p -> p.toString().endsWith(".dat"))
                    .forEach(p -> {
                        try {
                            String fileName = p.getFileName().toString();
                            String uuidStr = fileName.substring(0, fileName.length() - 4);
                            UUID uuid = UUID.fromString(uuidStr);
                            String name = resolveNameFromNativeCache(uuid);
                            profileCache.put(uuid, name);
                        } catch (Exception e) {
                            PrivitMod.LOGGER.warn("[PlayerIdentityCache] Failed to process file: {}", p, e);
                        }
                    });
        } catch (IOException e) {
            PrivitMod.LOGGER.error("[PlayerIdentityCache] Failed to scan playerdata directory", e);
        }
    }

    private static String resolveNameFromNativeCache(UUID uuid) {
        ServerPlayer player = server.getPlayerList().getPlayer(uuid);
        if (player != null) return player.getName().getString();

        return uuid.toString().substring(0, 8) + "...";
    }

    public static Optional<UUID> findUuidByName(String name) {
        if (name == null || name.isBlank()) return Optional.empty();
        String lower = name.toLowerCase(Locale.ROOT);
        return profileCache.entrySet().stream()
                .filter(e -> e.getValue() != null && e.getValue().toLowerCase(Locale.ROOT).equals(lower))
                .map(Map.Entry::getKey)
                .findFirst();
    }

    public static Map<UUID, String> searchPlayers(String query, int limit) {
        if (query == null || (query = query.trim()).isEmpty()) return Collections.emptyMap();

        limit = Math.min(Math.max(limit, 1), MAX_LIMIT);
        String lowerQuery = query.toLowerCase(Locale.ROOT);

        return profileCache.entrySet().stream()
                .filter(entry -> {
                    String name = entry.getValue();
                    return name != null && name.toLowerCase(Locale.ROOT).contains(lowerQuery);
                })
                .sorted(Comparator.comparing(entry -> {
                    String name = entry.getValue().toLowerCase(Locale.ROOT);
                    int index = name.indexOf(lowerQuery);
                    return index == -1 ? Integer.MAX_VALUE : index;
                }))
                .limit(limit)
                .collect(LinkedHashMap::new, (map, entry) -> map.put(entry.getKey(), entry.getValue()), Map::putAll);
    }

    public static Map<UUID, String> searchPlayers(String query) {
        return searchPlayers(query, DEFAULT_LIMIT);
    }

    private static void registerEvents() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, serverInstance) -> {
            ServerPlayer player = handler.getPlayer();
            UUID uuid = player.getUUID();
            String name = player.getName().getString();
            profileCache.put(uuid, name);
        });
    }

    public static void reload() {
        profileCache.clear();
        loadProfiles();
    }

    public static String getNameByUuid(UUID uuid) {
        String cached = profileCache.get(uuid);
        if (cached != null) return cached;

        String resolved = resolveNameFromNativeCache(uuid);
        if (resolved != null && !resolved.endsWith("...")) {
            profileCache.put(uuid, resolved);
        }
        return resolved;
    }

    public static void updateName(UUID uuid, String name) {
        if (name != null && !name.isBlank()) {
            profileCache.put(uuid, name);
        }
    }
}