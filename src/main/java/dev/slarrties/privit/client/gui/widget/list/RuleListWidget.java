package dev.slarrties.privit.client.gui.widget.list;

import dev.slarrties.privit.client.util.FormattedTooltipText;
import dev.slarrties.privit.common.region.rule.Rule;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;

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
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        clearPendingTooltip();
        super.extractWidgetRenderState(graphics, mouseX, mouseY, delta);
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

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {}

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
        public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int index, int y, int x, int entryWidth, int entryHeight,
                                             int mouseX, int mouseY, boolean hovered, boolean selected, float a) {
            this.currentX = x;
            this.currentY = y;
            this.currentWidth = entryWidth;
            this.currentHeight = entryHeight;

            Minecraft minecraft = Minecraft.getInstance();
            boolean isEnabled = isRuleEnabledGetter != null && isRuleEnabledGetter.apply(data);

            Component ruleName = data.getName();
            int maxTextWidth = entryWidth - 15;
            String trimmedStr = minecraft.font.plainSubstrByWidth(ruleName.getString(), maxTextWidth);
            if (!trimmedStr.equals(ruleName.getString()) && trimmedStr.length() > 3) {
                trimmedStr = trimmedStr.substring(0, trimmedStr.length() - 3) + "...";
            }

            int textColor = hovered ? 0xFFFFFFFF : 0xFFF5F5F5;
            graphics.text(
                    minecraft.font,
                    Component.literal(trimmedStr),
                    x + 6,
                    y + (entryHeight - 8) / 2,
                    textColor,
                    true
            );

            String icon = isEnabled ? "✔" : "✘";
            int iconColor = isEnabled ? 0xFF55FF77 : 0xFFFF5555;
            int iconX = x + entryWidth - 1 - minecraft.font.width(icon);
            int iconY = y + (entryHeight - 8) / 2;

            graphics.text(
                    minecraft.font,
                    Component.literal(icon).withStyle(s -> s.withColor(iconColor)),
                    iconX,
                    iconY,
                    iconColor,
                    true
            );

            graphics.fill(x + 6, y + entryHeight - 1, x + entryWidth - 6, y + entryHeight, 0x10FFFFFF);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            if (onClick != null) {
                Minecraft minecraft = Minecraft.getInstance();
                SoundEvent soundEvent = SoundEvents.VILLAGER_WORK_CARTOGRAPHER;
                minecraft.getSoundManager().play(
                        SimpleSoundInstance.forUI(soundEvent, 1.0F, 1.0F)
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