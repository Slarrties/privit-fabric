package dev.slarrties.privit.client.render;

public enum RenderType {
    CONFLICT(100),
    ORIGINAL(50),
    DRAFT(10);

    private final int priority;

    RenderType(int priority) { this.priority = priority; }

    public int getPriority() { return priority; }

    public static int compare(RenderType a, RenderType b) {
        return Integer.compare(b.getPriority(), a.getPriority());
    }
}