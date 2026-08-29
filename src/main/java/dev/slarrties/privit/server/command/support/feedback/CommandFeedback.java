package dev.slarrties.privit.server.command.support.feedback;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.server.command.ServerCommandSource;

public final class CommandFeedback {

    private CommandFeedback() {}

    public static int success(ServerCommandSource source, Text message) {
        source.sendFeedback(() -> message, true);
        return 1;
    }

    public static int success(ServerCommandSource source, String message) {
        return success(source, Text.literal(message));
    }

    public static int error(ServerCommandSource source, Text message) {
        source.sendError(message);
        return 0;
    }

    public static int error(ServerCommandSource source, String message) {
        return error(source, Text.literal(message).formatted(Formatting.RED));
    }

    public static void info(ServerCommandSource source, Text message) {
        source.sendFeedback(() -> message, false);
    }

    public static void info(ServerCommandSource source, String message) {
        info(source, Text.literal(message));
    }
}