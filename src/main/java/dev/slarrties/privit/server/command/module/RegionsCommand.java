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

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

import java.util.List;
import java.util.UUID;

public final class RegionsCommand implements CommandModule, PlayerAccess, OperatorAccess {

    @Override
    public void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        root.then(CommandManager.literal("regions")
                .executes(this::own)
                .then(PlayerIdentityArgument.player("player")
                        .requires(CommandPermissions::operator)
                        .executes(this::other)));
    }

    @Override
    public List<HelpLine> helpLines() {
        return List.of(
                new HelpLine("privit.command.help.regions", HelpLine.Access.PLAYER),
                new HelpLine("privit.command.help.regions_other", HelpLine.Access.OPERATOR)
        );
    }

    private int own(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource source = ctx.getSource();
        ServerPlayerEntity player = PlayerIdentityResolver.requirePlayer(source);

        return sendList(source, player.getUuid(), player.getName().getString());
    }

    private int other(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource source = ctx.getSource();
        UUID uuid = PlayerIdentityArgument.getUuid(ctx, "player");
        String name = PlayerIdentityResolver.resolveName(uuid);

        return sendList(source, uuid, name);
    }

    private int sendList(ServerCommandSource source, UUID playerUuid, String playerName) {
        List<RegionLookup.LocatedRegion> owned = RegionLookup.findOwned(source.getServer(), playerUuid);

        if (owned.isEmpty()) {
            return CommandFeedback.success(
                    source, Text.translatable("privit.command.regions.empty", playerName)
            );
        }

        CommandFeedback.info(source, Text.translatable("privit.command.regions.header", playerName).formatted(Formatting.GOLD));

        int index = 1;
        for (RegionLookup.LocatedRegion entry : owned) {
            Text line = Text.literal(index + ". ")
                    .formatted(Formatting.GRAY)
                    .append(Text.literal(entry.region().name()).formatted(Formatting.WHITE))
                    .append(Text.literal("  "))
                    .append(Text.literal(CommandText.dimensionName(entry.world())).formatted(Formatting.DARK_GRAY))
                    .append(Text.literal("  "))
                    .append(CommandText.clickableBlockPos(entry.region().pivotPos()))
                    .append(Text.literal("  "))
                    .append(CommandText.clickableUuid(entry.region().id()));

            CommandFeedback.info(source, line);
            index++;
        }

        return owned.size();
    }
}