package dev.slarrties.privit.server.command.support.argument;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import net.minecraft.text.Text;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

import java.util.UUID;

public final class PlayerIdentityArgument {

    private static final SimpleCommandExceptionType PLAYER_NOT_FOUND =
            new SimpleCommandExceptionType(Text.translatable("privit.command.regions.player_not_found", ""));

    private static final SuggestionProvider<ServerCommandSource> ONLINE_PLAYERS = (context, builder) -> {
        String remaining = builder.getRemainingLowerCase();

        for (ServerPlayerEntity player : context.getSource().getServer().getPlayerManager().getPlayerList()) {
            String name = player.getName().getString();
            if (name.toLowerCase().startsWith(remaining)) {
                builder.suggest(name);
            }
        }
        return builder.buildFuture();
    };

    private PlayerIdentityArgument() {
    }

    public static RequiredArgumentBuilder<ServerCommandSource, String> player(String name) {
        return CommandManager.argument(name, StringArgumentType.word())
                .suggests(ONLINE_PLAYERS);
    }

    public static UUID getUuid(CommandContext<ServerCommandSource> context, String name)
            throws CommandSyntaxException {
        String input = StringArgumentType.getString(context, name);

        return PlayerIdentityResolver.resolveUuid(context.getSource(), input)
                .orElseThrow(() -> new SimpleCommandExceptionType(
                        Text.translatable("privit.command.regions.player_not_found", input)
                ).create());
    }

    public static String getRaw(CommandContext<ServerCommandSource> context, String name) {
        return StringArgumentType.getString(context, name);
    }
}