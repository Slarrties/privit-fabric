package dev.slarrties.privit.server.tracking.context;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

public final class FluidPlacementContext {

    private static final ThreadLocal<FluidPlacementContext> CURRENT = new ThreadLocal<>();
    private final ServerPlayerEntity player;
    private final BlockPos pos;

    private FluidPlacementContext(ServerPlayerEntity player, BlockPos pos) {
        this.player = player;
        this.pos = pos;
    }

    public static void push(ServerPlayerEntity player, BlockPos pos) {
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

    public ServerPlayerEntity getPlayer() {
        return player;
    }

    public BlockPos getPos() {
        return pos;
    }
}