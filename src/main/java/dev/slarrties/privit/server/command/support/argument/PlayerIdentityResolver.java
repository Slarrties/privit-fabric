package dev.slarrties.privit.server.command.support.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;

import dev.slarrties.privit.server.identity.PlayerIdentityCache;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.command.ServerCommandSource;

import java.util.UUID;
import java.util.Optional;

public final class PlayerIdentityResolver {

    private PlayerIdentityResolver() {}

    public static Optional<ServerPlayerEntity> getPlayer(ServerCommandSource source) {
        return Optional.ofNullable(source.getPlayer());
    }

    public static ServerPlayerEntity requirePlayer(ServerCommandSource source) throws CommandSyntaxException {
        return source.getPlayerOrThrow();
    }

    public static Optional<UUID> resolveUuid(ServerCommandSource source, String input) {
        if (input == null || input.isBlank()) {
            return Optional.empty();
        }

        MinecraftServer server = source.getServer();

        ServerPlayerEntity online = server.getPlayerManager().getPlayer(input);
        if (online != null) {
            return Optional.of(online.getUuid());
        }

        Optional<UUID> fromCache = PlayerIdentityCache.findUuidByName(input);
        if (fromCache.isPresent()) {
            return fromCache;
        }

        try {
            return Optional.of(UUID.fromString(input));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    public static String resolveName(UUID uuid) {
        String name = PlayerIdentityCache.getNameByUuid(uuid);
        if (name != null && !name.isBlank()) {
            return name;
        }
        return uuid.toString().substring(0, 8) + "...";
    }
}