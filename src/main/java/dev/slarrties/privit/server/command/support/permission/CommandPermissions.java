package dev.slarrties.privit.server.command.support.permission;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.permissions.Permission;

public final class CommandPermissions {

    public static final Permission MANAGE_REGIONS = Permission.Atom.create("privit.manage");
    public static final Permission VIEW_REGIONS = Permission.Atom.create("privit.view");

    private CommandPermissions() {}

    public static boolean canManage(CommandSourceStack source) {
        return source.permissions().hasPermission(MANAGE_REGIONS);
    }

    public static boolean canView(CommandSourceStack source) {
        return source.permissions().hasPermission(VIEW_REGIONS);
    }

    public static boolean operator(CommandSourceStack source) {
        return canManage(source);
    }

    public static boolean atLeast(CommandSourceStack source, Permission permission) {
        return source.permissions().hasPermission(permission);
    }
}