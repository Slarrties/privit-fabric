package dev.slarrties.privit.server.command.support.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;

import dev.slarrties.privit.server.identity.PlayerIdentityCache;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.commands.CommandSourceStack;

import java.util.UUID;
import java.util.Optional;

public final class PlayerIdentityResolver {
    private PlayerIdentityResolver() {}

    public static Optional<ServerPlayer> getPlayer(CommandSourceStack source) {
        return Optional.ofNullable(source.getPlayer());
    }

    public static ServerPlayer requirePlayer(CommandSourceStack source) throws CommandSyntaxException {
        return source.getPlayerOrException();
    }

    public static Optional<UUID> resolveUuid(CommandSourceStack source, String input) {
        if (input == null || input.isBlank()) return Optional.empty();

        ServerPlayer online = source.getServer().getPlayerList().getPlayerByName(input);
        if (online != null) return Optional.of(online.getUUID());

        Optional<UUID> fromCache = PlayerIdentityCache.findUuidByName(input);
        if (fromCache.isPresent()) return fromCache;

        try {
            return Optional.of(UUID.fromString(input));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    public static String resolveName(UUID uuid) {
        String name = PlayerIdentityCache.getNameByUuid(uuid);
        if (name != null && !name.isBlank()) return name;

        return uuid.toString().substring(0, 8) + "...";
    }
}