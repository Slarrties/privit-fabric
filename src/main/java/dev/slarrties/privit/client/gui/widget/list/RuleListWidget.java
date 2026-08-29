package dev.slarrties.privit.client.gui.widget.list;

import dev.slarrties.privit.client.util.FormattedTooltipText;
import dev.slarrties.privit.common.region.rule.Rule;

import net.minecraft.text.Text;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;

import java.util.List;
import java.util.function.Function;
import java.util.function.BiConsumer;

public class RuleListWidget extends BaseScrollableListWidget<Rule> {

    private final Function<Rule, Boolean> isRuleEnabledGetter;
    private final BiConsumer<Rule, Boolean> onRuleToggle;

    public RuleListWidget(
            int x, int y, int width, int height, int itemHeight,
            Function<Rule, Boolean> isRuleEnabledGetter,
            BiConsumer<Rule, Boolean> onRuleToggle
    ) {
        super(x, y, width, height, itemHeight, null, null, null);
        this.isRuleEnabledGetter = isRuleEnabledGetter;
        this.onRuleToggle = onRuleToggle;
        setScrollbarPadding(0);
        setContentPadding(0);
    }

    public void refreshEntries(List<Rule> rulesToShow) {
        entries.clear();

        for (Rule rule : rulesToShow)
            entries.add(new RuleEntry(rule));
    }

    @Override
    protected void addDataEntries(List<Rule> data) {}

    @Override
    protected Entry<Rule> createAddEntry() { return null; }

    @Override
    protected boolean shouldSelectFirstByDefault() { return false; }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {}

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        clearPendingTooltip();
        super.renderWidget(context, mouseX, mouseY, delta);

        if (!isMouseOver(mouseX, mouseY)) return;

        for (Entry<Rule> entry : entries) {
            if (!(entry instanceof RuleEntry re) || !re.isMouseOver(mouseX, mouseY)) continue;

            setPendingTooltip(
                    FormattedTooltipText.of(re.data.getDescription()),
                    mouseX, mouseY
            );
            break;
        }
    }

    // ────────────────────────────────────────────────────────────────
    //
    // ────────────────────────────────────────────────────────────────

    private class RuleEntry extends Entry<Rule> {

        private int currentX, currentY, currentWidth, currentHeight;

        public RuleEntry(Rule rule) {
            super(rule, null);

            this.onClick = () -> {
                if (onRuleToggle != null) {
                    boolean current = isRuleEnabledGetter.apply(data);
                    onRuleToggle.accept(data, !current);
                }
            };
        }

        @Override
        public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight,
                           int mouseX, int mouseY, boolean hovered, boolean selected, float tickDelta) {

            this.currentX = x;
            this.currentY = y;
            this.currentWidth = entryWidth;
            this.currentHeight = entryHeight;

            MinecraftClient client = MinecraftClient.getInstance();
            boolean isEnabled = isRuleEnabledGetter != null && isRuleEnabledGetter.apply(data);
            Text ruleName = data.getName();
            int maxTextWidth = entryWidth - 15;
            String trimmedStr = client.textRenderer.trimToWidth(ruleName, maxTextWidth).getString();

            if (!trimmedStr.equals(ruleName.getString()) && trimmedStr.length() > 3) {
                trimmedStr = trimmedStr.substring(0, trimmedStr.length() - 3) + "...";
            }

            int textColor = hovered ? 0xFFFFFF : 0xF5F5F5;

            context.drawTextWithShadow(
                    client.textRenderer,
                    Text.literal(trimmedStr),
                    x + 6,
                    y + (entryHeight - 8) / 2,
                    textColor
            );

            String icon = isEnabled ? "✔" : "✗";
            int iconColor = isEnabled ? 0x55FF77 : 0xFF5555;

            int iconX = x + entryWidth - 1 - client.textRenderer.getWidth(icon);
            int iconY = y + (entryHeight - 8) / 2;

            context.drawTextWithShadow(
                    client.textRenderer,
                    Text.literal(icon).styled(s -> s.withColor(iconColor)),
                    iconX, iconY, iconColor
            );
            context.fill(x + 6, y + entryHeight - 1, x + entryWidth - 6, y + entryHeight, 0x10FFFFFF);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (onClick != null) {
                MinecraftClient client = MinecraftClient.getInstance();
                SoundEvent soundEvent = SoundEvents.ENTITY_VILLAGER_WORK_CARTOGRAPHER;

                client.getSoundManager().play(
                        PositionedSoundInstance.master(soundEvent, 1.0F, 1.0F)
                );

                onClick.run();
                return true;
            }
            return false;
        }

        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            return mouseX >= currentX && mouseX <= currentX + currentWidth &&
                    mouseY >= currentY && mouseY <= currentY + currentHeight;
        }

        @Override
        public void setFocused(boolean focused) {}

        @Override
        public boolean isFocused() {
            return false;
        }
    }
}