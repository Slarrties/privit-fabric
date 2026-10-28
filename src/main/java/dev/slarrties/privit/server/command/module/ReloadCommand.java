package dev.slarrties.privit.server.command.module;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import dev.slarrties.privit.common.config.ConfigManager;
import dev.slarrties.privit.server.command.support.CommandModule;
import dev.slarrties.privit.server.command.support.help.HelpLine;
import dev.slarrties.privit.server.command.support.access.OperatorAccess;
import dev.slarrties.privit.server.command.support.feedback.CommandFeedback;
import dev.slarrties.privit.server.command.support.permission.CommandPermissions;

import net.minecraft.network.chat.Component;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;

import java.util.List;

public final class ReloadCommand implements CommandModule, OperatorAccess {
    @Override
    public void register(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("reload")
                .requires(CommandPermissions::canManage)
                .executes(ctx -> {
                    try {
                        ConfigManager.reload();
                        return CommandFeedback.success(
                                ctx.getSource(),
                                Component.translatable("privit.command.reload.success")
                        );
                    } catch (Exception e) {
                        return CommandFeedback.error(
                                ctx.getSource(),
                                Component.translatable("privit.command.reload.fail", e.getMessage())
                        );
                    }
                }));
    }

    @Override
    public List<HelpLine> helpLines() {
        return List.of(new HelpLine("privit.command.help.reload", HelpLine.Access.OPERATOR));
    }
}