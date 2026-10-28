package dev.slarrties.privit.server.command.module;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import dev.slarrties.privit.server.command.support.CommandModule;
import dev.slarrties.privit.server.command.support.help.HelpLine;
import dev.slarrties.privit.server.command.support.query.RegionLookup;
import dev.slarrties.privit.server.command.support.format.CommandText;
import dev.slarrties.privit.server.command.support.access.PlayerAccess;
import dev.slarrties.privit.server.command.support.access.OperatorAccess;
import dev.slarrties.privit.server.command.support.feedback.CommandFeedback;
import dev.slarrties.privit.server.command.support.argument.PlayerIdentityResolver;
import dev.slarrties.privit.server.command.support.argument.PlayerIdentityArgument;
import dev.slarrties.privit.server.command.support.permission.CommandPermissions;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;

import java.util.List;
import java.util.UUID;

public final class RegionsCommand implements CommandModule, PlayerAccess, OperatorAccess {
    @Override
    public void register(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("regions")
                .executes(this::own)
                .then(PlayerIdentityArgument.player("player")
                        .requires(CommandPermissions::canManage)
                        .executes(this::other)));
    }

    @Override
    public List<HelpLine> helpLines() {
        return List.of(
                new HelpLine("privit.command.help.regions", HelpLine.Access.PLAYER),
                new HelpLine("privit.command.help.regions_other", HelpLine.Access.OPERATOR)
        );
    }

    private int own(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = PlayerIdentityResolver.requirePlayer(source);
        return sendList(source, player.getUUID(), player.getName().getString());
    }

    private int other(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        UUID uuid = PlayerIdentityArgument.getUuid(ctx, "player");
        String name = PlayerIdentityResolver.resolveName(uuid);
        return sendList(source, uuid, name);
    }

    private int sendList(CommandSourceStack source, UUID playerUuid, String playerName) {
        List<RegionLookup.LocatedRegion> owned = RegionLookup.findOwned(source.getServer(), playerUuid);
        if (owned.isEmpty()) {
            return CommandFeedback.success(
                    source, Component.translatable("privit.command.regions.empty", playerName)
            );
        }
        CommandFeedback.info(source, Component.translatable("privit.command.regions.header", playerName).withStyle(ChatFormatting.GOLD));
        int index = 1;
        for (RegionLookup.LocatedRegion entry : owned) {
            Component line = Component.literal(index + ". ")
                    .withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(entry.region().name()).withStyle(ChatFormatting.WHITE))
                    .append(Component.literal("  "))
                    .append(Component.literal(CommandText.dimensionName(entry.level())).withStyle(ChatFormatting.DARK_GRAY))
                    .append(Component.literal("  "))
                    .append(CommandText.clickableBlockPos(entry.region().pivotPos()))
                    .append(Component.literal("  "))
                    .append(CommandText.clickableUuid(entry.region().id()));
            CommandFeedback.info(source, line);
            index++;
        }
        return owned.size();
    }
}