package dev.slarrties.privit.server.command.support.help;

import dev.slarrties.privit.server.command.support.permission.CommandPermissions;

import net.minecraft.text.Text;
import net.minecraft.server.command.ServerCommandSource;

public record HelpLine(String translationKey, Access access) {

    public enum Access {
        PLAYER,
        OPERATOR
    }

    public boolean visibleFor(ServerCommandSource source) {
        return access == Access.PLAYER || CommandPermissions.operator(source);
    }

    public Text text() {
        return Text.translatable(translationKey);
    }
}