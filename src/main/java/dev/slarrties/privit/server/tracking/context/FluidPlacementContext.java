package dev.slarrties.privit.server.tracking.context;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

public final class FluidPlacementContext {

    private static final ThreadLocal<FluidPlacementContext> CURRENT = new ThreadLocal<>();
    private final ServerPlayer player;
    private final BlockPos pos;

    private FluidPlacementContext(ServerPlayer player, BlockPos pos) {
        this.player = player;
        this.pos = pos;
    }

    public static void push(ServerPlayer player, BlockPos pos) {
        if (player == null || pos == null) return;
        CURRENT.set(new FluidPlacementContext(player, pos));
    }

    public static void pop() {
        CURRENT.remove();
    }

    @Nullable
    public static FluidPlacementContext getCurrent() {
        return CURRENT.get();
    }

    public ServerPlayer getPlayer() {
        return player;
    }

    public BlockPos getPos() {
        return pos;
    }
}