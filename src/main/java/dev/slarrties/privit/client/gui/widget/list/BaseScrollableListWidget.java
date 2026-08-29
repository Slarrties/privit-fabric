package dev.slarrties.privit.client.gui.widget.list;

import dev.slarrties.privit.PrivitMod;
import dev.slarrties.privit.client.util.FormattedTooltipText;

import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;

import java.util.List;
import java.util.ArrayList;
import java.util.function.Consumer;

public abstract class BaseScrollableListWidget<T> extends ClickableWidget implements Drawable, Selectable {

    public enum ScrollbarPosition { RIGHT, LEFT, NONE }

    private static final Identifier SCROLLER_TEXTURE = Identifier.of(PrivitMod.MOD_ID, "textures/gui/book_scrollbar_thumb.png");
    private static final Identifier SCROLLER_BACKGROUND_TEXTURE = Identifier.of(PrivitMod.MOD_ID, "textures/gui/book_scrollbar_track.png");

    private FormattedTooltipText pendingTooltip = null;
    private int tooltipX, tooltipY;

    protected final List<Entry<T>> entries = new ArrayList<>();
    protected Entry<T> selected = null;
    protected Entry<T> hovered = null;

    protected ScrollbarPosition scrollbarPosition = ScrollbarPosition.RIGHT;
    protected double scrollAmount = 0;
    protected int scrollbarWidth = 6;
    protected int scrollbarPadding = 0;
    protected int contentPadding = 0;
    protected int itemHeight = 20;

    protected final Consumer<T> onItemSelected;
    protected final Consumer<T> onItemToggled;
    protected final Runnable onAddClicked;

    public BaseScrollableListWidget(int x, int y, int width, int height, int itemHeight,
                                    Consumer<T> onItemSelected, Consumer<T> onItemToggled, Runnable onAddClicked) {
        super(x, y, width, height, Text.empty());
        this.itemHeight = itemHeight;
        this.onItemSelected = onItemSelected;
        this.onItemToggled = onItemToggled;
        this.onAddClicked = onAddClicked;
    }

    public void setItemHeight(int height) { this.itemHeight = height; }
    public void setScrollbarPosition(ScrollbarPosition pos) { this.scrollbarPosition = pos; }
    public void setScrollbarWidth(int width) { this.scrollbarWidth = width; }
    public void setScrollbarPadding(int padding) { this.scrollbarPadding = padding; }
    public void setContentPadding(int padding) { this.contentPadding = padding; }

    public void updateEntries(List<T> data) {
        entries.clear();
        addDataEntries(data);
        if (onAddClicked != null) {
            entries.add(createAddEntry());
        }

        if (!entries.isEmpty() && shouldSelectFirstByDefault()) {
            setSelected(entries.get(0));
        }

        scrollAmount = 0;
    }

    protected abstract void addDataEntries(List<T> data);
    protected abstract Entry<T> createAddEntry();
    protected boolean shouldSelectFirstByDefault() { return true; }

    public void setSelected(Entry<T> entry) {
        this.selected = entry;
        if (entry != null && entry.data != null && onItemSelected != null) {
            onItemSelected.accept(entry.data);
        }
    }

    protected void setPendingTooltip(FormattedTooltipText tooltip, int mouseX, int mouseY) {
        this.pendingTooltip = tooltip;
        this.tooltipX = mouseX;
        this.tooltipY = mouseY;
    }

    // -------------------------------------------------------------------------
    // Render
    // -------------------------------------------------------------------------

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        context.enableScissor(getX(), getY(), getX() + width, getY() + height);

        int contentX = getX() + getContentLeftOffset();
        int contentWidth = getContentWidth();
        int drawY = getY() - (int) scrollAmount;
        hovered = null;

        for (int i = 0; i < entries.size(); i++) {
            Entry<T> entry = entries.get(i);
            int entryY = drawY + i * itemHeight;

            if (entryY + itemHeight >= getY() && entryY <= getY() + height) {
                boolean isHovered = mouseX >= contentX && mouseX <= contentX + contentWidth &&
                        mouseY >= entryY && mouseY < entryY + itemHeight;
                boolean isSelected = (selected != null && selected == entry);

                entry.render(context, i, entryY, contentX, contentWidth, itemHeight,
                        mouseX, mouseY, isHovered, isSelected, delta);

                if (isHovered) hovered = entry;
            }
        }

        context.disableScissor();
        renderScrollbar(context);
    }

    private void renderScrollbar(DrawContext context) {
        if (scrollbarPosition == ScrollbarPosition.NONE || getMaxScroll() <= 0) return;

        int scrollbarX = getScrollbarX();
        int maxScroll = getMaxScroll();
        int thumbHeight = MathHelper.clamp((this.height * this.height) / getTotalHeight(), 32, this.height - 8);
        int thumbY = (int)(scrollAmount * (this.height - thumbHeight) / maxScroll) + this.getY();

        thumbY = MathHelper.clamp(thumbY, this.getY(), this.getY() + this.height - thumbHeight);
        context.drawTexture(SCROLLER_TEXTURE, scrollbarX, thumbY, 5, 5, scrollbarWidth, thumbHeight);
    }

    protected void clearPendingTooltip() {
        pendingTooltip = null;
    }

    public void renderPendingTooltip(DrawContext context) {
        if (pendingTooltip == null) return;

        context.drawTooltip(
                net.minecraft.client.MinecraftClient.getInstance().textRenderer,
                pendingTooltip.text(),
                tooltipX,
                tooltipY + 25
        );
    }

    private int getScrollbarX() {
        if (scrollbarPosition == ScrollbarPosition.LEFT) {
            return this.getX() + scrollbarPadding;
        }
        return this.getX() + this.width - scrollbarWidth - scrollbarPadding;
    }

    private int getContentLeftOffset() {
        return (scrollbarPosition == ScrollbarPosition.LEFT && getMaxScroll() > 0) ? scrollbarWidth + scrollbarPadding + contentPadding : contentPadding;
    }

    private int getContentWidth() {
        return this.width - (contentPadding * 2) - (getMaxScroll() > 0 ? scrollbarWidth + scrollbarPadding : 0);
    }

    private int getTotalHeight() {
        return entries.size() * itemHeight;
    }

    private int getMaxScroll() {
        return Math.max(0, getTotalHeight() - this.height);
    }

    // -------------------------------------------------------------------------
    // Events
    // -------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isMouseOver(mouseX, mouseY)) return false;

        if (isOverScrollbar(mouseX, mouseY)) {
            updateScrollFromMouse(mouseY);
            return true;
        }

        int contentX = getX() + getContentLeftOffset();
        int contentWidth = getContentWidth();
        int drawY = getY() - (int) scrollAmount;

        for (int i = 0; i < entries.size(); i++) {
            Entry<T> entry = entries.get(i);
            int entryY = drawY + i * itemHeight;

            if (entryY + itemHeight >= getY() && entryY <= getY() + height) {
                if (mouseY >= entryY && mouseY < entryY + itemHeight &&
                        mouseX >= contentX && mouseX <= contentX + contentWidth) {
                    if (entry.mouseClicked(mouseX, mouseY, button)) {
                        setSelected(entry);
                        return true;
                    }
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (isOverScrollbar(mouseX, mouseY)) {
            updateScrollFromMouse(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (this.isMouseOver(mouseX, mouseY)) {
            scrollAmount = MathHelper.clamp(scrollAmount - vertical * itemHeight / 2, 0, getMaxScroll());
            return true;
        }
        return false;
    }

    private boolean isOverScrollbar(double mouseX, double mouseY) {
        if (scrollbarPosition == ScrollbarPosition.NONE || getMaxScroll() <= 0) return false;

        int sx = getScrollbarX();
        return mouseX >= sx && mouseX <= sx + scrollbarWidth &&
                mouseY >= this.getY() && mouseY <= this.getY() + this.height;
    }

    private void updateScrollFromMouse(double mouseY) {
        double relative = (mouseY - this.getY()) / (double) this.height;
        scrollAmount = MathHelper.clamp(relative * getTotalHeight(), 0, getMaxScroll());
    }

    @Override
    public SelectionType getType() {
        return hovered != null ? SelectionType.HOVERED : SelectionType.NONE;
    }

    // -------------------------------------------------------------------------
    //  Entry
    // -------------------------------------------------------------------------

    public abstract static class Entry<E> implements Element {
        protected final E data;
        protected Runnable onClick;

        public Entry(E data, Runnable onClick) {
            this.data = data;
            this.onClick = onClick;
        }

        public abstract void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight,
                                    int mouseX, int mouseY, boolean hovered, boolean selected, float tickDelta);

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (onClick != null) onClick.run();
            return true;
        }
    }
}