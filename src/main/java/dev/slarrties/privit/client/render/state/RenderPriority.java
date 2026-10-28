package dev.slarrties.privit.client.render.state;

public enum RenderPriority {
    CONFLICT(100),
    ORIGINAL(50),
    DRAFT(10);

    private final int priority;

    RenderPriority(int priority) { this.priority = priority; }

    public int getPriority() { return priority; }

    public static int compare(RenderPriority a, RenderPriority b) {
        return Integer.compare(b.getPriority(), a.getPriority());
    }
}