package dev.slarrties.privit.server.tracking.context;

import java.util.UUID;
import net.minecraft.util.math.BlockPos;

public final class PistonMovementContext {

    private static final ThreadLocal<PistonMovementContext> CURRENT = new ThreadLocal<>();

    private final UUID responsible;
    private final BlockPos pistonPos;

    private PistonMovementContext(UUID responsible, BlockPos pistonPos) {
        this.responsible = responsible;
        this.pistonPos = pistonPos;
    }

    public static void push(UUID playerUuid, BlockPos pos) {
        CURRENT.set(new PistonMovementContext(playerUuid, pos));
    }

    public static void pop() {
        CURRENT.remove();
    }

    public static PistonMovementContext getCurrent() {
        return CURRENT.get();
    }

    public UUID getResponsible() {
        return responsible;
    }

    public BlockPos getPistonPos() {
        return pistonPos;
    }
}