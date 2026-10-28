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

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;

import java.util.Set;
import java.util.List;
import java.util.UUID;

public final class HereCommand implements CommandModule, OperatorAccess {
    @Override
    public void register(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("here")
                .requires(CommandPermissions::canManage)
                .executes(this::here));
    }

    @Override
    public List<HelpLine> helpLines() {
        return List.of(new HelpLine("privit.command.help.here", HelpLine.Access.OPERATOR));
    }

    private int here(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = PlayerIdentityResolver.requirePlayer(source);
        var regionOpt = WorldRegistry.get(player.level())
                .getRegionManager()
                .getAt(player.blockPosition());

        if (regionOpt.isEmpty()) {
            return CommandFeedback.success(
                    source,
                    Component.translatable("privit.command.here.none")
            );
        }

        Region region = regionOpt.get();
        CommandFeedback.info(source,
                Component.translatable("privit.command.here.header", region.name())
                        .withStyle(ChatFormatting.GOLD));
        CommandFeedback.info(source, Component.literal("UUID: ").withStyle(ChatFormatting.GRAY)
                .append(CommandText.clickableUuid(region.id())));

        Set<UUID> owners = region.groups().findByName("owner")
                .map(RegionPlayerGroup::getMembers)
                .orElse(Set.of());

        if (owners.isEmpty()) {
            CommandFeedback.info(source, Component.translatable("privit.command.here.no_owners")
                    .withStyle(ChatFormatting.RED));
        } else {
            CommandFeedback.info(source, Component.translatable("privit.command.here.owners")
                    .withStyle(ChatFormatting.GRAY));
            for (UUID ownerUuid : owners) {
                String name = PlayerIdentityResolver.resolveName(ownerUuid);
                CommandFeedback.info(source, Component.literal("  • ")
                        .withStyle(ChatFormatting.DARK_GRAY)
                        .append(CommandText.clickablePlayerName(name, ownerUuid)));
            }
        }
        return 1;
    }
}