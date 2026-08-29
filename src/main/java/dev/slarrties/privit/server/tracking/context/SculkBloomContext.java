package dev.slarrties.privit.server.tracking.context;

import java.util.UUID;
import org.jetbrains.annotations.Nullable;

public final class SculkBloomContext {

    private static final ThreadLocal<SculkBloomContext> CURRENT = new ThreadLocal<>();

    private final UUID responsible;

    private SculkBloomContext(UUID responsible) {
        this.responsible = responsible;
    }

    public static void push(UUID responsible) {
        if (responsible == null) return;
        CURRENT.set(new SculkBloomContext(responsible));
    }

    public static void pop() {
        CURRENT.remove();
    }

    @Nullable
    public static SculkBloomContext getCurrent() {
        return CURRENT.get();
    }

    @Nullable
    public static UUID getResponsible() {
        SculkBloomContext ctx = CURRENT.get();
        return ctx != null ? ctx.responsible : null;
    }
}