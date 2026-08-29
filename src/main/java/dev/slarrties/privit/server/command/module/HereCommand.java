package dev.slarrties.privit.server.command.module;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import dev.slarrties.privit.common.region.RegionPlayerGroup;
import dev.slarrties.privit.server.region.Region;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.command.support.CommandModule;
import dev.slarrties.privit.server.command.support.help.HelpLine;
import dev.slarrties.privit.server.command.support.format.CommandText;
import dev.slarrties.privit.server.command.support.access.OperatorAccess;
import dev.slarrties.privit.server.command.support.feedback.CommandFeedback;
import dev.slarrties.privit.server.command.support.argument.PlayerIdentityResolver;
import dev.slarrties.privit.server.command.support.permission.CommandPermissions;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

import java.util.Set;
import java.util.List;
import java.util.UUID;

public final class HereCommand implements CommandModule, OperatorAccess {

    @Override
    public void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        root.then(CommandManager.literal("here")
                .requires(CommandPermissions::operator)
                .executes(this::here));
    }

    @Override
    public List<HelpLine> helpLines() {
        return List.of(new HelpLine("privit.command.help.here", HelpLine.Access.OPERATOR));
    }

    private int here(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource source = ctx.getSource();
        ServerPlayerEntity player = PlayerIdentityResolver.requirePlayer(source);

        var regionOpt = WorldRegistry.get(player.getServerWorld())
                .getRegionManager()
                .getAt(player.getBlockPos());

        if (regionOpt.isEmpty()) {
            return CommandFeedback.success(
                    source,
                    Text.translatable("privit.command.here.none")
            );
        }

        Region region = regionOpt.get();

        CommandFeedback.info(source,
                Text.translatable("privit.command.here.header", region.name())
                        .formatted(Formatting.GOLD));

        CommandFeedback.info(source, Text.literal("UUID: ").formatted(Formatting.GRAY)
                .append(CommandText.clickableUuid(region.id())));

        Set<UUID> owners = region.groups().findByName("owner")
                .map(RegionPlayerGroup::getMembers)
                .orElse(Set.of());

        if (owners.isEmpty()) {
            CommandFeedback.info(source, Text.translatable("privit.command.here.no_owners")
                    .formatted(Formatting.RED));
        } else {
            CommandFeedback.info(source, Text.translatable("privit.command.here.owners")
                    .formatted(Formatting.GRAY));

            for (UUID ownerUuid : owners) {
                String name = PlayerIdentityResolver.resolveName(ownerUuid);
                CommandFeedback.info(source, Text.literal("  • ")
                        .formatted(Formatting.DARK_GRAY)
                        .append(CommandText.clickablePlayerName(name, ownerUuid)));
            }
        }

        return 1;
    }
}