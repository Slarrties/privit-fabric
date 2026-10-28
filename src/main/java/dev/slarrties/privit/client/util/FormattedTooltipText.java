package dev.slarrties.privit.client.util;

import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

public final class FormattedTooltipText {
    private final List<Component> lines;

    public FormattedTooltipText(Component raw) {
        if (raw == null || raw.getString().isEmpty()) {
            this.lines = Collections.singletonList(Component.empty());
            return;
        }

        String rawString = raw.getString();
        String[] parts = rawString.split("\n");
        List<Component> result = new ArrayList<>(parts.length);

        for (String part : parts) {
            String trimmed = part.trim();

            if (!trimmed.isEmpty()) {
                result.add(Component.literal(trimmed));
            }
        }

        this.lines = result.isEmpty()
                ? Collections.singletonList(raw)
                : Collections.unmodifiableList(result);
    }

    public List<Component> text() {
        return lines;
    }

    public static FormattedTooltipText empty() {
        return new FormattedTooltipText(Component.empty());
    }

    public static FormattedTooltipText of(Component raw) {
        return new FormattedTooltipText(raw);
    }
}