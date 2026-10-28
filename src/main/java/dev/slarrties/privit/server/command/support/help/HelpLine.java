package dev.slarrties.privit.server.command.support.help;

import dev.slarrties.privit.server.command.support.permission.CommandPermissions;

import net.minecraft.network.chat.Component;
import net.minecraft.commands.CommandSourceStack;

public record HelpLine(String translationKey, Access access) {
    public enum Access { PLAYER, OPERATOR }

    public boolean visibleFor(CommandSourceStack source) {
        return access == Access.PLAYER || CommandPermissions.operator(source);
    }

    public Component text() {
        return Component.translatable(translationKey);
    }
}