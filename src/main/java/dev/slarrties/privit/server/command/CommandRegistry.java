package dev.slarrties.privit.server.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import dev.slarrties.privit.server.command.module.*;
import dev.slarrties.privit.server.command.support.help.HelpLine;
import dev.slarrties.privit.server.command.support.CommandModule;
import dev.slarrties.privit.server.command.support.feedback.CommandFeedback;

import net.minecraft.text.Text;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.command.CommandManager.RegistrationEnvironment;

import java.util.List;

public final class CommandRegistry {

    public static final String ROOT = "privit";
    private static final List<CommandModule> MODULES = List.of(
            new HelpCommand(),
            new ReloadCommand(),
            new RegionsCommand(),
            new DeleteCommand(),
            new GridCommand(),
//            new AddOwnerCommand(),
            new HereCommand()
    );

    private CommandRegistry() {}

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher,
                                CommandRegistryAccess registryAccess,
                                RegistrationEnvironment environment) {

        LiteralArgumentBuilder<ServerCommandSource> root = CommandManager.literal(ROOT);

        root.executes(ctx -> showHelp(ctx.getSource()));

        for (CommandModule module : MODULES) module.register(root);

        dispatcher.register(root);
    }

    public static int showHelp(ServerCommandSource source) {
        CommandFeedback.info(source, Text.translatable("privit.command.help.header"));

        for (CommandModule module : MODULES) {
            for (HelpLine line : module.helpLines()) {
                if (line.visibleFor(source)) {
                    CommandFeedback.info(source, line.text());
                }
            }
        }
        return 1;
    }
}