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

import net.minecraft.text.Text;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.command.argument.UuidArgumentType;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import java.util.List;
import java.util.UUID;

public final class GridCommand implements CommandModule, PlayerAccess, OperatorAccess {

    @Override
    public void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        root.then(CommandManager.literal("grid")
                .executes(this::hideAll)
                .then(CommandManager.argument("region", UuidArgumentType.uuid())
                        .then(CommandManager.literal("off").executes(ctx -> setOne(ctx, false)))
                        .then(CommandManager.literal("on").executes(ctx -> setOne(ctx, true)))));
    }

    @Override
    public List<HelpLine> helpLines() {
        return List.of(
                new HelpLine("privit.command.help.grid", HelpLine.Access.PLAYER),
                new HelpLine("privit.command.help.grid_one", HelpLine.Access.PLAYER)
        );
    }

    private int hideAll(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity player = PlayerIdentityResolver.requirePlayer(ctx.getSource());

        for (ServerWorld world : ctx.getSource().getServer().getWorlds()) {
            WorldRegistry.get(world).getGridSubscriptions().onPlayerLeave(player.getUuid());
        }

        ServerPlayNetworking.send(player, new RegionGridClearS2CPacket());
        return CommandFeedback.success(
                ctx.getSource(),
                Text.translatable("privit.command.grid.cleared")
        );
    }

    private int setOne(CommandContext<ServerCommandSource> ctx, boolean enabled)
            throws CommandSyntaxException {
        ServerCommandSource source = ctx.getSource();
        ServerPlayerEntity player = PlayerIdentityResolver.requirePlayer(source);
        UUID regionId = UuidArgumentType.getUuid(ctx, "region");

        var found = RegionLookup.findById(source.getServer(), regionId);
        if (found.isEmpty()) {
            return CommandFeedback.error(
                    source,
                    Text.translatable("privit.command.grid.not_found", regionId.toString())
            );
        }

        RegionLookup.LocatedRegion located = found.get();
        Region region = located.region();

        if (!CommandPermissions.operator(source) && !region.isOwner(player.getUuid())) {
            return CommandFeedback.error(
                    source,
                    Text.translatable("privit.command.grid.not_owner")
            );
        }

        ServerWorld regionWorld = located.world();
        var grids = WorldRegistry.get(regionWorld).getGridSubscriptions();
        var session = WorldRegistry.get(regionWorld).getRegionGuiSessions().find(regionId);

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
                Text.translatable(
                        enabled ? "privit.command.grid.shown" : "privit.command.grid.hidden",
                        region.name()
                )
        );
    }
}