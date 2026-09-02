package dev.slarrties.privit.server.tracking.context;

import java.util.UUID;

import net.minecraft.util.math.BlockPos;

public final class BlockFallContext {

    private static final ThreadLocal<BlockFallContext> CURRENT = new ThreadLocal<>();

    private final UUID responsible;
    private final BlockPos originPos;

    private BlockFallContext(UUID responsible, BlockPos originPos) {
        this.responsible = responsible;
        this.originPos = originPos;
    }

    public static void push(UUID playerUuid, BlockPos pos) {
        CURRENT.set(new BlockFallContext(playerUuid, pos));
    }

    public static void push(UUID playerUuid) {
        push(playerUuid, null);
    }

    public static void pop() {
        CURRENT.remove();
    }

    public static BlockFallContext getCurrent() {
        return CURRENT.get();
    }

    public UUID getResponsible() {
        return responsible;
    }

    public BlockPos getOriginPos() {
        return originPos;
    }
}