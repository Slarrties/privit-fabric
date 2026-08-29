package dev.slarrties.privit.server.command.support.format;

import net.minecraft.text.Text;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.server.world.ServerWorld;

import java.util.UUID;

public final class CommandText {

    private CommandText() {}

    public static Text clickableUuid(UUID uuid) {
        return Text.literal("UUID")
                .styled(style -> style
                        .withColor(Formatting.AQUA)
                        .withUnderline(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, uuid.toString()))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(uuid.toString())))
                );
    }

    public static Text clickableBlockPos(BlockPos pos) {
        String cmd = String.format("/tp @s %d %d %d", pos.getX(), pos.getY(), pos.getZ());
        String label = String.format("[%d, %d, %d]", pos.getX(), pos.getY(), pos.getZ());

        return Text.literal(label)
                .styled(style -> style
                        .withColor(Formatting.GREEN)
                        .withUnderline(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, cmd))
                        .withHoverEvent(new HoverEvent(
                                HoverEvent.Action.SHOW_TEXT,
                                Text.translatable("privit.command.regions.tp_hint", pos.getX(), pos.getY(), pos.getZ())
                        )));
    }

    public static Text clickablePlayerName(String name, UUID uuid) {
        return Text.literal(name)
                .styled(style -> style
                        .withColor(Formatting.YELLOW)
                        .withUnderline(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, uuid.toString()))
                        .withHoverEvent(new HoverEvent(
                                HoverEvent.Action.SHOW_TEXT,
                                Text.literal(uuid.toString())
                        )));
    }

    public static String dimensionName(ServerWorld world) {
        return world.getRegistryKey().getValue().getPath();
    }
}