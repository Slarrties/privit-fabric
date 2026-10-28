package dev.slarrties.privit.server.command.module;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import dev.slarrties.privit.common.network.payload.s2c.RegionGridClearS2CPacket;
import dev.slarrties.privit.common.network.payload.s2c.RegionGridStateS2CPacket;
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

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.UuidArgument;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import java.util.List;
import java.util.UUID;

public final class GridCommand implements CommandModule, PlayerAccess, OperatorAccess {
    @Override
    public void register(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("grid")
                .executes(this::hideAll)
                .then(Commands.argument("region", UuidArgument.uuid())
                        .then(Commands.literal("off").executes(ctx -> setOne(ctx, false)))
                        .then(Commands.literal("on").executes(ctx -> setOne(ctx, true)))));
    }

    @Override
    public List<HelpLine> helpLines() {
        return List.of(
                new HelpLine("privit.command.help.grid", HelpLine.Access.PLAYER),
                new HelpLine("privit.command.help.grid_one", HelpLine.Access.PLAYER)
        );
    }

    private int hideAll(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = PlayerIdentityResolver.requirePlayer(ctx.getSource());
        for (ServerLevel level : ctx.getSource().getServer().getAllLevels()) {
            WorldRegistry.get(level).getGridSubscriptions().onPlayerLeave(player.getUUID());
        }
        ServerPlayNetworking.send(player, new RegionGridClearS2CPacket());
        return CommandFeedback.success(
                ctx.getSource(),
                Component.translatable("privit.command.grid.cleared")
        );
    }

    private int setOne(CommandContext<CommandSourceStack> ctx, boolean enabled)
            throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = PlayerIdentityResolver.requirePlayer(source);
        UUID regionId = UuidArgument.getUuid(ctx, "region");
        var found = RegionLookup.findById(source.getServer(), regionId);
        if (found.isEmpty()) {
            return CommandFeedback.error(
                    source,
                    Component.translatable("privit.command.grid.not_found", regionId.toString())
            );
        }
        RegionLookup.LocatedRegion located = found.get();
        Region region = located.region();
        if (!CommandPermissions.canManage(source) && !region.isOwner(player.getUUID())) {
            return CommandFeedback.error(
                    source,
                    Component.translatable("privit.command.grid.not_owner")
            );
        }

        ServerLevel regionLevel = located.level();
        var grids = WorldRegistry.get(regionLevel).getGridSubscriptions();
        var session = WorldRegistry.get(regionLevel).getRegionGuiSessions().find(regionId);

        if (enabled) {
            grids.subscribe(regionId, player);
            RegionGridStateS2CPacket packet;
            if (session != null) {
                var state = session.state();
                packet = RegionGridStateS2CPacket.show(
                        state.getId(),
                        state.getColor(),
                        state.getRealBounds(),
                        state.getDraftBounds(),
                        state.getConflictBounds()
                );
            } else {
                packet = RegionGridStateS2CPacket.show(
                        region.id(),
                        region.color(),
                        region.bounds(),
                        region.bounds(),
                        List.of()
                );
            }
            ServerPlayNetworking.send(player, packet);
        } else {
            grids.unsubscribe(regionId, player);
            ServerPlayNetworking.send(player, RegionGridStateS2CPacket.hide(regionId));
        }

        return CommandFeedback.success(
                source,
                Component.translatable(
                        enabled ? "privit.command.grid.shown" : "privit.command.grid.hidden",
                        region.name()
                )
        );
    }
}