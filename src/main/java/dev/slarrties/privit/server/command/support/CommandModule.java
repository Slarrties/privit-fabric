package dev.slarrties.privit.server.command.support;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.slarrties.privit.server.command.support.help.HelpLine;
import net.minecraft.server.command.ServerCommandSource;
import java.util.List;

public interface CommandModule {
    void register(LiteralArgumentBuilder<ServerCommandSource> root);

    default List<HelpLine> helpLines() {
        return List.of();
    }
}