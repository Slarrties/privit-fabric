package dev.slarrties.privit.server.command.support.feedback;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.commands.CommandSourceStack;

public final class CommandFeedback {
    private CommandFeedback() {}

    public static int success(CommandSourceStack source, Component message) {
        source.sendSuccess(() -> message, true);
        return 1;
    }

    public static int success(CommandSourceStack source, String message) {
        return success(source, Component.literal(message));
    }

    public static int error(CommandSourceStack source, Component message) {
        source.sendFailure(message);
        return 0;
    }

    public static int error(CommandSourceStack source, String message) {
        return error(source, Component.literal(message).withStyle(ChatFormatting.RED));
    }

    public static void info(CommandSourceStack source, Component message) {
        source.sendSuccess(() -> message, false);
    }

    public static void info(CommandSourceStack source, String message) {
        info(source, Component.literal(message));
    }
}