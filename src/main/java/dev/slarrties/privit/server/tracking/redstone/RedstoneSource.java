package dev.slarrties.privit.server.tracking.redstone;

import net.minecraft.world.World;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface RedstoneSource {

    boolean isSource(@NotNull BlockState state,
                     @NotNull World world,
                     @NotNull BlockPos pos);
}