package dev.slarrties.privit.client.util;

import dev.slarrties.privit.PrivitMod;
import dev.slarrties.privit.common.util.PlayerIdentity;
import dev.slarrties.privit.common.util.PlayerNameProvider;
import dev.slarrties.privit.common.region.RegionPlayerGroup;
import dev.slarrties.privit.common.network.payload.c2s.RequestPlayerNamesC2SPacket;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientPlayerIdentityCache implements PlayerNameProvider {

    private static final ClientPlayerIdentityCache INSTANCE = new ClientPlayerIdentityCache();
    private final Map<UUID, PlayerIdentity> cache = new ConcurrentHashMap<>();

    private ClientPlayerIdentityCache() {}

    public static ClientPlayerIdentityCache getInstance() { return INSTANCE; }

    @Override
    public PlayerIdentity getIdentity(UUID uuid) {
        if (uuid == null) return PlayerIdentity.unknown(UUID.randomUUID());

        return cache.computeIfAbsent(uuid, PlayerIdentity::unknown);
    }

    @Override
    public void updateName(UUID uuid, String name) {
        if (uuid == null || name == null || name.isBlank()) return;

        PlayerIdentity oldIdentity = cache.get(uuid);
        PlayerIdentity newIdentity = (oldIdentity != null)
                ? oldIdentity.withName(name)
                : new PlayerIdentity(uuid, name);

        cache.put(uuid, newIdentity);
    }

    public boolean requestMissingNames(Collection<UUID> uuids) {
        Set<UUID> missing = new HashSet<>();

        for (UUID uuid : uuids) {
            PlayerIdentity identity = cache.get(uuid);

            if (identity == null || !identity.isKnown()) {
                missing.add(uuid);
            }
        }

        if (missing.isEmpty()) return false;

        ClientPlayNetworking.send(new RequestPlayerNamesC2SPacket(missing));
        return true;
    }

    public Collection<PlayerIdentity> getAllKnownIdentities() {
        return cache.values();
    }

    public void updateFromSearchResults(Map<UUID, String> searchResults) {
        for (Map.Entry<UUID, String> entry : searchResults.entrySet()) {
            UUID uuid = entry.getKey();
            String name = entry.getValue();

            if (uuid == null || name == null || name.isBlank()) {
                continue;
            }

            PlayerIdentity existing = cache.get(uuid);
            PlayerIdentity updated = (existing != null)
                    ? existing.withName(name)
                    : new PlayerIdentity(uuid, name);

            cache.put(uuid, updated);
        }
    }

    public void updateFromPacket(Map<UUID, String> names) {
        for (var entry : names.entrySet()) {
            updateName(entry.getKey(), entry.getValue());
        }
    }

    public void clear() { cache.clear(); }

    public Set<UUID> collectAllUuids(List<RegionPlayerGroup> groups) {
        Set<UUID> all = new HashSet<>();

        for (RegionPlayerGroup group : groups)
            all.addAll(group.getMembers());

        return all;
    }
}