package dev.slarrties.privit.server.command.support.argument;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;

import java.util.UUID;

public final class PlayerIdentityArgument {
    private static final SimpleCommandExceptionType PLAYER_NOT_FOUND =
            new SimpleCommandExceptionType(Component.translatable("privit.command.regions.player_not_found", ""));

    private static final SuggestionProvider<CommandSourceStack> ONLINE_PLAYERS = (context, builder) -> {
        String remaining = builder.getRemainingLowerCase();

        for (ServerPlayer player : context.getSource().getServer().getPlayerList().getPlayers()) {
            String name = player.getName().getString();

            if (name.toLowerCase().startsWith(remaining)) {
                builder.suggest(name);
            }
        }
        return builder.buildFuture();
    };

    private PlayerIdentityArgument() {
    }

    public static RequiredArgumentBuilder<CommandSourceStack, String> player(String name) {
        return Commands.argument(name, StringArgumentType.word()).suggests(ONLINE_PLAYERS);
    }

    public static UUID getUuid(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        String input = StringArgumentType.getString(context, name);
        return PlayerIdentityResolver.resolveUuid(context.getSource(), input)
                .orElseThrow(() -> new SimpleCommandExceptionType(Component.translatable("privit.command.regions.player_not_found", input)).create());
    }

    public static String getRaw(CommandContext<CommandSourceStack> context, String name) {
        return StringArgumentType.getString(context, name);
    }
}