package dev.slarrties.privit.server.tracking.context;

import net.minecraft.util.math.BlockPos;

public final class FireSpreadContext {

    private static final ThreadLocal<FireSpreadContext> CURRENT = new ThreadLocal<>();
    private final long sourceFirePos;
    private final long targetPos;

    private FireSpreadContext(BlockPos source, BlockPos target) {
        this.sourceFirePos = source.asLong();
        this.targetPos = target.asLong();
    }

    public static void push(BlockPos sourceFirePos, BlockPos targetPos) {
        if (sourceFirePos == null || targetPos == null) return;
        CURRENT.set(new FireSpreadContext(sourceFirePos, targetPos));
    }

    public static void pop() { CURRENT.remove(); }

    public static FireSpreadContext getCurrent() { return CURRENT.get(); }

    public BlockPos getSourcePos() { return BlockPos.fromLong(sourceFirePos); }

    public BlockPos getTargetPos() { return BlockPos.fromLong(targetPos); }
}