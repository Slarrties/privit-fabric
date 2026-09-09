package dev.slarrties.privit.server.tracking.context;

import net.minecraft.util.math.BlockPos;
import java.util.UUID;

public final class BoneMealUseContext {
    private static final ThreadLocal<BoneMealUseContext> CURRENT = new ThreadLocal<>();

    private final UUID responsible;
    private final BlockPos origin;

    private BoneMealUseContext(UUID responsible, BlockPos origin) {
        this.responsible = responsible;
        this.origin = origin;
    }

    public static void push(UUID responsible, BlockPos origin) {
        CURRENT.set(new BoneMealUseContext(responsible, origin));
    }

    public static void pop() {
        CURRENT.remove();
    }

    public static BoneMealUseContext getCurrent() {
        return CURRENT.get();
    }

    public UUID getResponsible() { return responsible; }
}