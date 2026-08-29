package dev.slarrties.privit.client.util;

import net.minecraft.text.Text;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

public final class FormattedTooltipText {

    private final List<Text> lines;

    public FormattedTooltipText(Text raw) {
        if (raw == null || raw.getString().isEmpty()) {
            this.lines = Collections.singletonList(Text.empty());
            return;
        }

        String rawString = raw.getString();
        String[] parts = rawString.split("\n");
        List<Text> result = new ArrayList<>(parts.length);

        for (String part : parts) {
            String trimmed = part.trim();

            if (!trimmed.isEmpty())
                result.add(Text.literal(trimmed));
        }

        this.lines = result.isEmpty()
                ? Collections.singletonList(raw)
                : Collections.unmodifiableList(result);
    }

    public List<Text> text() { return lines; }

    public static FormattedTooltipText empty() { return new FormattedTooltipText(Text.empty()); }

    public static FormattedTooltipText of(Text raw) { return new FormattedTooltipText(raw); }
}