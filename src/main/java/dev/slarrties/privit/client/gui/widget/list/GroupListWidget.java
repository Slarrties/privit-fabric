package dev.slarrties.privit.client.gui.widget.list;

import dev.slarrties.privit.PrivitMod;
import dev.slarrties.privit.client.util.FormattedTooltipText;
import dev.slarrties.privit.common.region.RegionPlayerGroup;

import net.minecraft.text.Text;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;

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
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {}

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        clearPendingTooltip();
        super.renderWidget(context, mouseX, mouseY, delta);

        if (!isMouseOver(mouseX, mouseY)) return;

        for (Entry<RegionPlayerGroup> entry : entries) {
            if (!(entry instanceof GroupEntry ge)) continue;
            if (!ge.isMouseOver(mouseX, mouseY) || ge.data == null) continue;

            if (ge.data.isOwnerGroup()) {
                setPendingTooltip(
                        FormattedTooltipText.of(Text.translatable("privit.gui.groups.owner.tooltip")),
                        mouseX, mouseY
                );
            } else if (ge.data.isVisitorsGroup()) {
                setPendingTooltip(
                        FormattedTooltipText.of(Text.translatable("privit.gui.groups.visitors.tooltip")),
                        mouseX, mouseY
                );
            }
            break;
        }
    }

    public static class GroupEntry extends Entry<RegionPlayerGroup> {

        private final boolean isAddButton;
        private int currentX, currentY, currentWidth, currentHeight;

        public GroupEntry(RegionPlayerGroup group, Runnable onClick) {
            super(group, onClick);
            this.isAddButton = group == null;
        }

        @Override
        public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight,
                           int mouseX, int mouseY, boolean hovered, boolean selected, float tickDelta) {
            this.currentX = x;
            this.currentY = y;
            this.currentWidth = entryWidth;
            this.currentHeight = entryHeight;

            int textX = x + 8;
            int textY = y + (entryHeight - 8) / 2;
            int textColor = getTextColor(hovered, selected);

            String displayName;
            if (isAddButton) {
                displayName = Text.translatable("privit.gui.groups.groups.add").getString();
            } else if (data != null && data.isOwnerGroup()) {
                displayName = Text.translatable("privit.gui.groups.groups.owner").getString();
            } else if (data != null && data.isVisitorsGroup()) {
                displayName = Text.translatable("privit.gui.groups.groups.visitors").getString();
            } else {
                displayName = (data == null ? "" : data.getName());
            }

            String trimmed = MinecraftClient.getInstance().textRenderer.trimToWidth(displayName, entryWidth - 16);
            Text text = Text.literal(trimmed).styled(style -> style.withBold(selected));

            context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, text, textX, textY, textColor);

            if (selected) {
                int underlineY = textY + 9;
                int underlineEnd = textX + MinecraftClient.getInstance().textRenderer.getWidth(trimmed);
                context.fill(textX, underlineY, underlineEnd, underlineY + 1, 0xFFFFFFFF);
            }

            context.fill(x + 6, y + entryHeight - 1, x + entryWidth - 6, y + entryHeight, 0x10FFFFFF);
        }

        private int getTextColor(boolean hovered, boolean selected) {
            if (data != null && data.isOwnerGroup()) {
                if (selected) return 0xFFE566;
                if (hovered)  return 0xFFEAA0;
                return 0xF5D76E;
            }

            if (data != null && data.isVisitorsGroup()) {
                if (selected) return 0xFFB84D;
                if (hovered)  return 0xFFC878;
                return 0xF0A04A;
            }

            if (isAddButton) {
                if (selected) return 0xFFFFFF;
                if (hovered)  return 0xA8FFC0;
                return 0x8AE0A2;
            }

            if (selected) return 0xFFFFFF;
            if (hovered)  return 0xFFFFFF;
            return 0xF5F5F5;
        }

        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            return mouseX >= currentX && mouseX <= currentX + currentWidth
                    && mouseY >= currentY && mouseY <= currentY + currentHeight;
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (onClick != null) {
                MinecraftClient client = MinecraftClient.getInstance();

                SoundEvent soundEvent = isAddButton
                        ? SoundEvents.ENTITY_VILLAGER_WORK_CARTOGRAPHER
                        : SoundEvents.ITEM_BOOK_PAGE_TURN;

                client.getSoundManager().play(
                        PositionedSoundInstance.master(soundEvent, 1.0F, 1.0F)
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