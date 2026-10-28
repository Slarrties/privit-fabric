package dev.slarrties.privit.server.command.support.format;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;

import java.util.UUID;

public final class CommandText {
    private CommandText() {}

    public static Component clickableUuid(UUID uuid) {
        return Component.literal("UUID")
                .withStyle(style -> style
                        .withColor(ChatFormatting.AQUA)
                        .withUnderlined(true)
                        .withClickEvent(new ClickEvent.CopyToClipboard(uuid.toString()))
                        .withHoverEvent(new HoverEvent.ShowText(Component.literal(uuid.toString())))
                );
    }

    public static Component clickableBlockPos(BlockPos pos) {
        String cmd = String.format("/tp @s %d %d %d", pos.getX(), pos.getY(), pos.getZ());
        String label = String.format("[%d, %d, %d]", pos.getX(), pos.getY(), pos.getZ());
        return Component.literal(label)
                .withStyle(style -> style
                        .withColor(ChatFormatting.GREEN)
                        .withUnderlined(true)
                        .withClickEvent(new ClickEvent.SuggestCommand(cmd))
                        .withHoverEvent(new HoverEvent.ShowText(
                                Component.translatable("privit.command.regions.tp_hint", pos.getX(), pos.getY(), pos.getZ())
                        )));
    }

    public static Component clickablePlayerName(String name, UUID uuid) {
        return Component.literal(name)
                .withStyle(style -> style
                        .withColor(ChatFormatting.YELLOW)
                        .withUnderlined(true)
                        .withClickEvent(new ClickEvent.CopyToClipboard(uuid.toString()))
                        .withHoverEvent(new HoverEvent.ShowText(
                                Component.literal(uuid.toString())
                        )));
    }

    public static String dimensionName(ServerLevel level) {
        return level.dimension().toString();
    }
}