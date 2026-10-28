package dev.slarrties.privit.client.gui.widget.list;

import dev.slarrties.privit.client.util.FormattedTooltipText;
import dev.slarrties.privit.common.region.RegionPlayerGroup;

import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.narration.NarrationElementOutput;

import java.util.List;
import java.util.Comparator;
import java.util.function.Consumer;

public class GroupListWidget extends BaseScrollableListWidget<RegionPlayerGroup> {

    private String selectedGroupName = null;

    public GroupListWidget(int x, int y, int width, int height, int itemHeight,
                           Consumer<RegionPlayerGroup> onGroupSelected,
                           Runnable onAddClicked) {
        super(x, y, width, height, itemHeight, onGroupSelected, null, onAddClicked);
        setScrollbarPosition(ScrollbarPosition.RIGHT);
        setScrollbarPadding(0);
        setContentPadding(0);
    }

    public void updateEntries(List<RegionPlayerGroup> groups, String selectedGroupName) {
        this.selectedGroupName = selectedGroupName;
        super.updateEntries(groups);
        restoreSelection();
    }

    @Override
    public void updateEntries(List<RegionPlayerGroup> groups) {
        updateEntries(groups, selectedGroupName);
    }

    @Override
    protected void addDataEntries(List<RegionPlayerGroup> data) {
        entries.clear();
        data.stream()
                .filter(RegionPlayerGroup::isOwnerGroup)
                .findFirst()
                .ifPresent(group -> entries.add(new GroupEntry(group, () -> {})));
        data.stream()
                .filter(RegionPlayerGroup::isVisitorsGroup)
                .findFirst()
                .ifPresent(group -> entries.add(new GroupEntry(group, () -> {})));
        data.stream()
                .filter(g -> !g.isOwnerGroup() && !g.isVisitorsGroup())
                .sorted(Comparator.comparing(RegionPlayerGroup::getName, String.CASE_INSENSITIVE_ORDER))
                .forEach(group -> entries.add(new GroupEntry(group, () -> {})));
    }

    private void restoreSelection() {
        if (selectedGroupName == null || entries.isEmpty()) {
            if (!entries.isEmpty()) setSelected(entries.get(0));
            return;
        }

        for (Entry<RegionPlayerGroup> entry : entries) {
            RegionPlayerGroup group = entry.data;

            if (group != null && group.getName().equalsIgnoreCase(selectedGroupName)) {
                setSelected(entry);
                return;
            }
        }

        if (!entries.isEmpty()) {
            setSelected(entries.get(0));
            selectedGroupName = entries.get(0).data != null
                    ? entries.get(0).data.getName()
                    : null;
        }
    }

    @Override
    protected Entry<RegionPlayerGroup> createAddEntry() {
        return new GroupEntry(null, onAddClicked);
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        clearPendingTooltip();
        super.extractWidgetRenderState(graphics, mouseX, mouseY, delta);

        if (!isMouseOver(mouseX, mouseY)) return;
        for (Entry<RegionPlayerGroup> entry : entries) {
            if (!(entry instanceof GroupEntry ge)) continue;
            if (!ge.isMouseOver(mouseX, mouseY) || ge.data == null) continue;
            if (ge.data.isOwnerGroup()) {
                setPendingTooltip(
                        FormattedTooltipText.of(Component.translatable("privit.gui.groups.owner.tooltip")),
                        mouseX, mouseY
                );
            } else if (ge.data.isVisitorsGroup()) {
                setPendingTooltip(
                        FormattedTooltipText.of(Component.translatable("privit.gui.groups.visitors.tooltip")),
                        mouseX, mouseY
                );
            }
            break;
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {}

    public static class GroupEntry extends Entry<RegionPlayerGroup> {
        private final boolean isAddButton;
        private int currentX, currentY, currentWidth, currentHeight;

        public GroupEntry(RegionPlayerGroup group, Runnable onClick) {
            super(group, onClick);
            this.isAddButton = group == null;
        }

        @Override
        public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int index, int y, int x, int entryWidth, int entryHeight,
                                             int mouseX, int mouseY, boolean hovered, boolean selected, float a) {
            this.currentX = x;
            this.currentY = y;
            this.currentWidth = entryWidth;
            this.currentHeight = entryHeight;
            int textX = x + 8;
            int textY = y + (entryHeight - 8) / 2;
            int textColor = getTextColor(hovered, selected);
            String displayName;
            if (isAddButton) {
                displayName = Component.translatable("privit.gui.groups.groups.add").getString();
            } else if (data != null && data.isOwnerGroup()) {
                displayName = Component.translatable("privit.gui.groups.groups.owner").getString();
            } else if (data != null && data.isVisitorsGroup()) {
                displayName = Component.translatable("privit.gui.groups.groups.visitors").getString();
            } else {
                displayName = (data == null ? "" : data.getName());
            }

            Minecraft minecraft = Minecraft.getInstance();
            String trimmed = minecraft.font.plainSubstrByWidth(displayName, entryWidth - 16);
            Component text = Component.literal(trimmed).withStyle(style -> style.withBold(selected));
            graphics.text(minecraft.font, text, textX, textY, textColor, true);

            if (selected) {
                int underlineY = textY + 9;
                int underlineEnd = textX + minecraft.font.width(trimmed);
                graphics.fill(textX, underlineY, underlineEnd, underlineY + 1, 0xFFFFFFFF);
            }
            graphics.fill(x + 6, y + entryHeight - 1, x + entryWidth - 6, y + entryHeight, 0x10FFFFFF);
        }

        private int getTextColor(boolean hovered, boolean selected) {
            if (data != null && data.isOwnerGroup()) {
                if (selected) return 0xFFFFE566;
                if (hovered)  return 0xFFFFEAA0;
                return 0xFFF5D76E;
            }
            if (data != null && data.isVisitorsGroup()) {
                if (selected) return 0xFFFFB84D;
                if (hovered)  return 0xFFFFC878;
                return 0xFFF0A04A;
            }
            if (isAddButton) {
                if (selected) return 0xFFFFFFFF;
                if (hovered)  return 0xFFA8FFC0;
                return 0xFF8AE0A2;
            }
            if (selected) return 0xFFFFFFFF;
            if (hovered)  return 0xFFFFFFFF;
            return 0xFFF5F5F5;
        }

        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            return mouseX >= currentX && mouseX <= currentX + currentWidth
                    && mouseY >= currentY && mouseY <= currentY + currentHeight;
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            if (onClick != null) {
                Minecraft client = Minecraft.getInstance();
                SoundEvent soundEvent = isAddButton
                        ? SoundEvents.VILLAGER_WORK_CARTOGRAPHER
                        : SoundEvents.BOOK_PAGE_TURN;

                client.getSoundManager().play(
                        SimpleSoundInstance.forUI(soundEvent, 1.0F, 1.0F)
                );
                onClick.run();
                return true;
            }
            return false;
        }

        @Override
        public void setFocused(boolean focused) {}

        @Override
        public boolean isFocused() { return false; }
    }
}