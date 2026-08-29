package dev.slarrties.privit.server.tracking.context;

import net.minecraft.util.math.BlockPos;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

public final class SpongePlacementContext {

    private static final ThreadLocal<SpongePlacementContext> CURRENT = new ThreadLocal<>();
    private final ServerPlayerEntity player;
    private final BlockPos pos;

    private SpongePlacementContext(ServerPlayerEntity player, BlockPos pos) {
        this.player = player;
        this.pos = pos;
    }

    public static void push(ServerPlayerEntity player, BlockPos pos) {
        if (player == null || pos == null) return;
        CURRENT.set(new SpongePlacementContext(player, pos));
    }

    public static void pop() { CURRENT.remove(); }

    @Nullable
    public static SpongePlacementContext getCurrent() { return CURRENT.get(); }

    public ServerPlayerEntity getPlayer() { return player; }

    public BlockPos getPos() { return pos; }
}