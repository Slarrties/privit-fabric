package dev.slarrties.privit.server.identity;

import dev.slarrties.privit.PrivitMod;

import com.google.gson.*;
import com.mojang.authlib.GameProfile;

import net.minecraft.util.UserCache;
import net.minecraft.util.WorldSavePath;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import java.util.*;
import java.nio.file.Path;
import java.nio.file.Files;
import java.io.IOException;

public final class PlayerIdentityCache {

    private static MinecraftServer server;
    private static Path cachePath;
    private static final Map<UUID, String> profileCache = new HashMap<>();
    private static final int DEFAULT_LIMIT = 500;
    private static final int MAX_LIMIT = 1000;

    private PlayerIdentityCache() {}

    public static void init(MinecraftServer serverInstance) {
        server = serverInstance;
        cachePath = server.getSavePath(WorldSavePath.ROOT).resolve("usercache.json");

        loadProfiles();
        registerEvents();
    }

    private static void loadProfiles() {
        if (Files.exists(cachePath)) {
            loadFromJson();
        } else {
            PrivitMod.LOGGER.info("[PlayerIdentityCache] usercache.json not found - try to playerdata scan");
            scanPlayerData();
        }
    }

    private static void loadFromJson() {
        try {
            String jsonContent = Files.readString(cachePath);
            JsonArray array = JsonParser.parseString(jsonContent).getAsJsonArray();

            for (JsonElement elem : array) {
                JsonObject obj = elem.getAsJsonObject();
                String name = obj.get("name").getAsString();
                String uuidStr = obj.get("uuid").getAsString();
                UUID uuid = UUID.fromString(uuidStr);
                profileCache.put(uuid, name);
            }

        } catch (IOException | JsonParseException e) {
            PrivitMod.LOGGER.error("[PlayerIdentityCache] Failed to load usercache.json — falling back to playerdata scan", e);
            scanPlayerData();
        }
    }

    private static void scanPlayerData() {
        Path playerDataDir = server.getSavePath(WorldSavePath.PLAYERDATA);
        if (!Files.exists(playerDataDir)) {
            PrivitMod.LOGGER.warn("[PlayerIdentityCache] playerdata directory not found — no profiles loaded");
            return;
        }

        UserCache userCache = server.getUserCache();

        try (var stream = Files.list(playerDataDir)) {
            stream.filter(p -> p.toString().endsWith(".dat"))
                    .forEach(p -> {
                        try {
                            String fileName = p.getFileName().toString();
                            String uuidStr = fileName.substring(0, fileName.length() - 4);
                            UUID uuid = UUID.fromString(uuidStr);
                            String name = getPlayerNameByUuid(uuid, userCache);

                            profileCache.put(uuid, name);
//                            PrivitMod.LOGGER.info("[PlayerIdentityCache] Loaded {} player profile from usercache.json {}", name, profileCache.size());
                        } catch (Exception e) {
                            PrivitMod.LOGGER.warn("[PlayerIdentityCache] Failed to process file: {}", p, e);
                        }
                    });
        } catch (IOException e) {
            PrivitMod.LOGGER.error("[PlayerIdentityCache] Failed to scan playerdata directory", e);
        }
    }

    private static String getPlayerNameByUuid(UUID uuid, UserCache userCache) {
        Optional<GameProfile> profileOpt = userCache.getByUuid(uuid);
        if (profileOpt.isPresent()) {
            String name = profileOpt.get().getName();
            if (name != null && !name.isEmpty()) {
                return name;
            }
        }

        ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);
        if (player != null) {
            return player.getName().getString();
        }

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

    public static Map<UUID, String> searchPlayers(String query, int limit, ServerPlayerEntity requester) {
        if (query == null || (query = query.trim()).isEmpty()) {
            return Collections.emptyMap();
        }

        limit = Math.min(Math.max(limit, 1), MAX_LIMIT);
        String lowerQuery = query.toLowerCase(Locale.ROOT);

        Map<UUID, String> results = profileCache.entrySet().stream()
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

        return results;
    }

    public static Map<UUID, String> searchPlayers(String query, ServerPlayerEntity requester) {
        return searchPlayers(query, DEFAULT_LIMIT, requester);
    }

    private static void registerEvents() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.player;
            UUID uuid = player.getUuid();
            String name = player.getName().getString();

            if (!name.equals(profileCache.get(uuid))) {
                profileCache.put(uuid, name);
//                PrivitMod.LOGGER.info("[PlayerIdentityCache] Updated player cache for {} ({})", name, uuid);
            }
        });
    }

    public static void reload() {
        profileCache.clear();
        loadProfiles();
    }

    public static String getNameByUuid(UUID uuid) {
        return profileCache.getOrDefault(uuid, null);
    }

    public static void updateName(UUID uuid, String name) {
        if (name != null && !name.isBlank()) {
            profileCache.put(uuid, name);
        }
    }
}