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

import net.minecraft.text.Text;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.command.argument.UuidArgumentType;

import java.util.List;
import java.util.UUID;

public final class DeleteCommand implements CommandModule, PlayerAccess, OperatorAccess {

    @Override
    public void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        root.then(CommandManager.literal("delete")
                .then(CommandManager.argument("region", UuidArgumentType.uuid())
                        .executes(this::delete)));
    }

    @Override
    public List<HelpLine> helpLines() {
        return List.of(
                new HelpLine("privit.command.help.delete", HelpLine.Access.PLAYER)
        );
    }

    private int delete(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource source = ctx.getSource();
        UUID regionId = UuidArgumentType.getUuid(ctx, "region");
        var found = RegionLookup.findById(source.getServer(), regionId);

        if (found.isEmpty()) {
            return CommandFeedback.error(
                    source,
                    Text.translatable("privit.command.delete.not_found", regionId.toString())
            );
        }

        RegionLookup.LocatedRegion located = found.get();
        Region region = located.region();

        if (!CommandPermissions.operator(source)) {
            ServerPlayerEntity player = PlayerIdentityResolver.requirePlayer(source);
            if (!region.isOwner(player.getUuid())) {
                return CommandFeedback.error(
                        source,
                        Text.translatable("privit.command.delete.not_owner")
                );
            }
        }

        String name = region.name();
        var result = located.manager().tryDelete(regionId, null);
        if (!result.isSuccess()) {
            return CommandFeedback.error(source, Text.translatable("privit.command.delete.failed", name));
        }

        WorldRegistry.get(located.world()).getRegionGuiSessions().close(regionId);
        WorldRegistry.get(located.world()).getGridSubscriptions()
                .hide(regionId, located.world().getPlayers());

        return CommandFeedback.success(source, Text.translatable("privit.command.delete.success", name, regionId.toString()));
    }
}