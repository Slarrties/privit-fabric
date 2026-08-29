package dev.slarrties.privit.server.command.module;

import dev.slarrties.privit.server.command.CommandRegistry;
import dev.slarrties.privit.server.command.support.CommandModule;
import dev.slarrties.privit.server.command.support.access.OperatorAccess;
import dev.slarrties.privit.server.command.support.help.HelpLine;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

import java.util.List;

public final class HelpCommand implements CommandModule, OperatorAccess {

    @Override
    public void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        root.then(CommandManager.literal("help").executes(ctx -> CommandRegistry.showHelp(ctx.getSource())));
    }

    @Override
    public List<HelpLine> helpLines() {
        return List.of(new HelpLine("privit.command.help.help", HelpLine.Access.PLAYER));
    }
}