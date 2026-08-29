package dev.slarrties.privit.common.util;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.Objects;

public record PlayerIdentity(
        @NotNull UUID uuid,
        String name,
        long lastUpdated // Is it necessary?
) {

    public PlayerIdentity {
        Objects.requireNonNull(uuid, "[PlayerIdentity] UUID cannot be null");
        if (uuid.equals(new UUID(0, 0))) {
            throw new IllegalArgumentException("[PlayerIdentity] null UUID is not allowed for PlayerIdentity");
        }
    }

    public PlayerIdentity(UUID uuid, String name) {
        this(uuid, name, System.currentTimeMillis());
    }

    public PlayerIdentity(UUID uuid) {
        this(uuid, null);
    }


    public String getDisplayName() {
        if (name != null && !name.isBlank()) return name;
        return uuid.toString().substring(0, 8) + "...";
    }

    public PlayerIdentity withName(String newName) {
        return new PlayerIdentity(uuid, newName, System.currentTimeMillis());
    }

    public boolean isKnown() {
        return name != null && !name.isBlank();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlayerIdentity that)) return false;
        return uuid.equals(that.uuid);
    }

    @Override
    public int hashCode() {
        return uuid.hashCode();
    }

    public static PlayerIdentity unknown(UUID uuid) {
        return new PlayerIdentity(uuid);
    }
}