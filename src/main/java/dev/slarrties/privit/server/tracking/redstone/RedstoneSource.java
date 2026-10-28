package dev.slarrties.privit.server.tracking.redstone;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface RedstoneSource {
    boolean isSource(
            @NotNull BlockState state,
            @NotNull Level level,
            @NotNull BlockPos pos
    );
}