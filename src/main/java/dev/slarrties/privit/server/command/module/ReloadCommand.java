package dev.slarrties.privit.server.command.module;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import dev.slarrties.privit.common.config.ConfigManager;
import dev.slarrties.privit.server.command.support.CommandModule;
import dev.slarrties.privit.server.command.support.help.HelpLine;
import dev.slarrties.privit.server.command.support.access.OperatorAccess;
import dev.slarrties.privit.server.command.support.feedback.CommandFeedback;
import dev.slarrties.privit.server.command.support.permission.CommandPermissions;

import net.minecraft.text.Text;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

import java.util.List;

public final class ReloadCommand implements CommandModule, OperatorAccess {

    @Override
    public void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        root.then(CommandManager.literal("reload")
                .requires(CommandPermissions::operator)
                .executes(ctx -> {
                    try {
                        ConfigManager.reload();
                        return CommandFeedback.success(
                                ctx.getSource(),
                                Text.translatable("privit.command.reload.success")
                        );
                    } catch (Exception e) {
                        return CommandFeedback.error(
                                ctx.getSource(),
                                Text.translatable("privit.command.reload.fail", e.getMessage())
                        );
                    }
                }));
    }

    @Override
    public List<HelpLine> helpLines() {
        return List.of(new HelpLine("privit.command.help.reload", HelpLine.Access.OPERATOR));
    }
}