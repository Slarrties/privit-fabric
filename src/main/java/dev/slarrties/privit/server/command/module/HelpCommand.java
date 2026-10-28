package dev.slarrties.privit.server.command.module;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import dev.slarrties.privit.server.command.CommandRegistry;
import dev.slarrties.privit.server.command.support.CommandModule;
import dev.slarrties.privit.server.command.support.help.HelpLine;
import dev.slarrties.privit.server.command.support.access.OperatorAccess;

import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;

import java.util.List;

public final class HelpCommand implements CommandModule, OperatorAccess {

    @Override
    public void register(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("help").executes(ctx -> CommandRegistry.showHelp(ctx.getSource())));
    }

    @Override
    public List<HelpLine> helpLines() {
        return List.of(new HelpLine("privit.command.help.help", HelpLine.Access.PLAYER));
    }
}