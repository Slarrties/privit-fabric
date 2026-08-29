package dev.slarrties.privit.server.command.support.permission;

import net.minecraft.server.command.ServerCommandSource;

public final class CommandPermissions {

    public static final int OPERATOR_LEVEL = 2;

    private CommandPermissions() {}

    public static boolean operator(ServerCommandSource source) {
        return source.hasPermissionLevel(OPERATOR_LEVEL);
    }

    public static boolean atLeast(ServerCommandSource source, int level) {
        return source.hasPermissionLevel(level);
    }
}