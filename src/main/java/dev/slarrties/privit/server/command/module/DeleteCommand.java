package dev.slarrties.privit.server.command.module;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import dev.slarrties.privit.server.region.Region;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.command.support.CommandModule;
import dev.slarrties.privit.server.command.support.help.HelpLine;
import dev.slarrties.privit.server.command.support.query.RegionLookup;
import dev.slarrties.privit.server.command.support.access.PlayerAccess;
import dev.slarrties.privit.server.command.support.access.OperatorAccess;
import dev.slarrties.privit.server.command.support.feedback.CommandFeedback;
import dev.slarrties.privit.server.command.support.argument.PlayerIdentityResolver;
import dev.slarrties.privit.server.command.support.permission.CommandPermissions;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.UuidArgument;

import java.util.List;
import java.util.UUID;

public final class DeleteCommand implements CommandModule, PlayerAccess, OperatorAccess {
    @Override
    public void register(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("delete")
                .then(Commands.argument("region", UuidArgument.uuid())
                        .executes(this::delete)));
    }

    @Override
    public List<HelpLine> helpLines() {
        return List.of(
                new HelpLine("privit.command.help.delete", HelpLine.Access.PLAYER)
        );
    }

    private int delete(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        UUID regionId = UuidArgument.getUuid(ctx, "region");
        var found = RegionLookup.findById(source.getServer(), regionId);
        if (found.isEmpty()) {
            return CommandFeedback.error(
                    source,
                    Component.translatable("privit.command.delete.not_found", regionId.toString())
            );
        }
        RegionLookup.LocatedRegion located = found.get();
        Region region = located.region();

        if (!CommandPermissions.canManage(source)) {
            ServerPlayer player = PlayerIdentityResolver.requirePlayer(source);
            if (!region.isOwner(player.getUUID())) {
                return CommandFeedback.error(
                        source,
                        Component.translatable("privit.command.delete.not_owner")
                );
            }
        }

        String name = region.name();
        var result = located.manager().tryDelete(regionId, null);
        if (!result.isSuccess()) {
            return CommandFeedback.error(source, Component.translatable("privit.command.delete.failed", name));
        }

        WorldRegistry.get(located.level()).getRegionGuiSessions().close(regionId);
        WorldRegistry.get(located.level()).getGridSubscriptions()
                .hide(regionId, located.level().players());

        return CommandFeedback.success(source, Component.translatable("privit.command.delete.success", name, regionId.toString()));
    }
}