package dev.slarrties.privit.server.command.module;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import dev.slarrties.privit.common.region.RegionGroups;
import dev.slarrties.privit.common.region.RegionPlayerGroup;
import dev.slarrties.privit.server.region.Region;
import dev.slarrties.privit.server.region.RegionManager;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.command.support.CommandModule;
import dev.slarrties.privit.server.command.support.help.HelpLine;
import dev.slarrties.privit.server.command.support.query.RegionLookup;
import dev.slarrties.privit.server.command.support.access.OperatorAccess;
import dev.slarrties.privit.server.command.support.feedback.CommandFeedback;
import dev.slarrties.privit.server.command.support.argument.PlayerIdentityResolver;
import dev.slarrties.privit.server.command.support.argument.PlayerIdentityArgument;
import dev.slarrties.privit.server.command.support.permission.CommandPermissions;

import net.minecraft.text.Text;
import net.minecraft.command.argument.UuidArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

import java.util.List;
import java.util.UUID;

public final class AddOwnerCommand implements CommandModule, OperatorAccess {

    @Override
    public void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        root.then(CommandManager.literal("addowner")
                .requires(CommandPermissions::operator)
                .then(CommandManager.argument("region", UuidArgumentType.uuid())
                        .then(PlayerIdentityArgument.player("player")
                                .executes(this::addOwner))));
    }

    @Override
    public List<HelpLine> helpLines() {
        return List.of(new HelpLine("privit.command.help.addowner", HelpLine.Access.OPERATOR));
    }

    private int addOwner(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource source = ctx.getSource();
        UUID regionId = UuidArgumentType.getUuid(ctx, "region");
        UUID playerUuid = PlayerIdentityArgument.getUuid(ctx, "player");
        String playerName = PlayerIdentityResolver.resolveName(playerUuid);

        var found = RegionLookup.findById(source.getServer(), regionId);
        if (found.isEmpty()) {
            return CommandFeedback.error(source,
                    Text.translatable("privit.command.addowner.region_not_found", regionId.toString()));
        }

        RegionLookup.LocatedRegion located = found.get();
        Region region = located.region();
        RegionGroups groups = region.groups();

        boolean alreadyOwner = groups.findByName("owner")
                .map(g -> g.getMembers().contains(playerUuid))
                .orElse(false);

        if (alreadyOwner) {
            return CommandFeedback.success(source,
                    Text.translatable("privit.command.addowner.already", playerName, region.name()));
        }

        RegionGroups updated = removeFromOtherGroups(groups, playerUuid);
        RegionPlayerGroup owner = new RegionPlayerGroup(
                updated.findByName("owner").orElseThrow()
        );
        owner.addMember(playerUuid);
        updated = updated.withUpdatedGroup("owner", owner);

        Region newRegion = new Region.Builder(region).groups(updated).build();
        RegionManager.OpResult result = WorldRegistry.get(located.world())
                .getRegionManager()
                .tryUpdate(region, newRegion, null);

        if (!result.isSuccess()) {
            return CommandFeedback.error(source,
                    Text.translatable("privit.command.addowner.failed", playerName, region.name()));
        }

        return CommandFeedback.success(source,
                Text.translatable("privit.command.addowner.success", playerName, region.name()));
    }

    private static RegionGroups removeFromOtherGroups(RegionGroups groups, UUID playerUuid) {
        RegionGroups result = groups;

        for (RegionPlayerGroup group : groups.getAll()) {
            if (group.isOwnerGroup() || !group.getMembers().contains(playerUuid)) {
                continue;
            }
            RegionPlayerGroup copy = new RegionPlayerGroup(group);
            copy.removeMember(playerUuid);
            result = result.withUpdatedGroup(group.getName(), copy);
        }

        return result;
    }
}