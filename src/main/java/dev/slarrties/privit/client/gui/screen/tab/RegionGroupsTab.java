package dev.slarrties.privit.client.gui.screen.tab;

import dev.slarrties.privit.PrivitMod;
import dev.slarrties.privit.client.gui.RegionGuiController;
import dev.slarrties.privit.client.gui.screen.AddPlayerScreen;
import dev.slarrties.privit.client.gui.screen.GroupEditScreen;
import dev.slarrties.privit.client.gui.widget.GuiButton;
import dev.slarrties.privit.client.gui.widget.list.GroupListWidget;
import dev.slarrties.privit.client.gui.widget.list.PlayerListWidget;
import dev.slarrties.privit.client.gui.widget.list.RuleListWidget;
import dev.slarrties.privit.client.util.ClientPlayerIdentityCache;
import dev.slarrties.privit.common.util.PlayerIdentity;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.region.rule.RuleSettings;
import dev.slarrties.privit.common.region.RegionGroups;
import dev.slarrties.privit.common.region.RegionPlayerGroup;
import dev.slarrties.privit.common.network.payload.c2s.RegionGuiUpdateC2SPacket;

import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.screen.ButtonTextures;
import net.minecraft.client.gui.widget.ClickableWidget;

import java.util.*;

public class RegionGroupsTab implements ITabPanel {

    private enum SubTab { RULES, PLAYERS }

    private SubTab currentSubTab = SubTab.RULES;
    private String selectedGroupName = null;
    private boolean isTabVisible = false;

    private final RegionGuiController controller;
    private final TextRenderer textRenderer;
    private final int x, y, width, height;
    private final int widthCenter;
    private final int rightX;

    private static final int MARGIN = 3;
    private static final int BUTTON_SIZE = 20;
    private static final int BUTTON_GAP = 8;

    private static final ButtonTextures STANDARD_BUTTON_BACKGROUND = new ButtonTextures(
            Identifier.of(PrivitMod.MOD_ID, "textures/gui/button_bg_common.png"),
            Identifier.of(PrivitMod.MOD_ID, "textures/gui/button_bg_common_pressed.png"),
            Identifier.of(PrivitMod.MOD_ID, "textures/gui/button_bg_common_active.png")
    );
    private static final Identifier RULES_ICON = Identifier.of(PrivitMod.MOD_ID, "textures/gui/rules_icon.png");
    private static final Identifier PLAYERS_ICON = Identifier.of(PrivitMod.MOD_ID, "textures/gui/players_icon.png");
    private static final Identifier EDIT_PROPERTIES_ICON = Identifier.of(PrivitMod.MOD_ID, "textures/gui/edit_properties_icon.png");

    private GroupListWidget groupList;
    private PlayerListWidget playerList;
    private RuleListWidget ruleList;
    private GuiButton subTabRulesButton;
    private GuiButton subTabPlayersButton;
    private GuiButton extraTabButton;
    private final List<ClickableWidget> widgets = new ArrayList<>();

    public RegionGroupsTab(RegionGuiController controller, TextRenderer textRenderer, int x, int y, int width, int height) {
        this.controller = controller;
        this.textRenderer = textRenderer;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.widthCenter = width / 2;
        this.rightX = x + widthCenter + MARGIN + 1;
    }

    @Override
    public void init(int x, int y, int width, int height) {
        initWidgets();

        RegionGroups currentGroups = controller.getLocalState().groups();
        List<RegionPlayerGroup> groupsList = currentGroups.getAll();

        if (!groupsList.isEmpty()) {
            selectedGroupName = groupsList.get(0).getName();
            groupList.updateEntries(new ArrayList<>(currentGroups.getAll()), selectedGroupName);
            updateRightContent();
        }

        updateButtonStates();

        widgets.add(subTabRulesButton);
        widgets.add(subTabPlayersButton);
        widgets.add(extraTabButton);

        if (groupList != null) widgets.add(groupList);
        if (playerList != null) widgets.add(playerList);
        if (ruleList != null) widgets.add(ruleList);

        updateVisibility();
    }

    private void initWidgets() {
        int listY = y + 25;
        int listHeight = Math.max(height - listY - 10, 95);
        int listWidth = width - widthCenter - MARGIN * 2;

        playerList = new PlayerListWidget(
                rightX,
                listY,
                listWidth - 11,
                listHeight,
                22,
                this::onPlayerRemove,
                this::onAddPlayerClicked,
                false
        );
        playerList.visible = false;

        ruleList = new RuleListWidget(
                rightX,
                listY,
                listWidth - 11,
                listHeight,
                22,
                rule -> getSelectedGroup().map(g -> g.isRuleEnabled(rule)).orElse(false),
                this::onRuleToggled
        );
        ruleList.visible = false;

        groupList = new GroupListWidget(
                x + MARGIN + 9,
                listY,
                widthCenter - MARGIN * 2 - 11,
                listHeight,
                22,
                this::onGroupSelected,
                this::addNewGroup
        );

        // ==================== buttons ====================

        int startX = rightX + 5;

        subTabRulesButton = new GuiButton.Builder(
                Text.empty(),
                btn -> switchSubTab(SubTab.RULES))
                .dimensions(startX, y, BUTTON_SIZE, BUTTON_SIZE)
                .tooltip(Tooltip.of(Text.translatable("privit.gui.groups.button.rules")))
                .icon(RULES_ICON, BUTTON_SIZE, BUTTON_SIZE, 0, -2)
                .setBackground(STANDARD_BUTTON_BACKGROUND)
                .build();

        subTabPlayersButton = new GuiButton.Builder(
                Text.empty(),
                btn -> switchSubTab(SubTab.PLAYERS))
                .dimensions(startX + BUTTON_SIZE + BUTTON_GAP, y, BUTTON_SIZE, BUTTON_SIZE)
                .tooltip(Tooltip.of(Text.translatable("privit.gui.groups.button.players")))
                .icon(PLAYERS_ICON, BUTTON_SIZE, BUTTON_SIZE, 0, -2)
                .setBackground(STANDARD_BUTTON_BACKGROUND)
                .build();

        extraTabButton = new GuiButton.Builder(Text.empty(), btn -> {
            getSelectedGroup().ifPresent(group -> {
                if (!group.isOwnerGroup() && !group.isVisitorsGroup()) {
                    MinecraftClient.getInstance().setScreen(new GroupEditScreen(controller, group));
                }
            });
        })
                .dimensions(startX + (BUTTON_SIZE + BUTTON_GAP) * 2, y, BUTTON_SIZE, BUTTON_SIZE)
                .tooltip(Tooltip.of(Text.translatable("privit.gui.groups.button.edit")))
                .icon(EDIT_PROPERTIES_ICON, BUTTON_SIZE, BUTTON_SIZE, 0, -2)
                .setBackground(STANDARD_BUTTON_BACKGROUND)
                .build();
    }

    private void addNewGroup() {
        String base = "group";
        int counter = 1;
        String newName = base + " " + counter;
        RegionGroups currentGroups = controller.getLocalState().groups();

        while (currentGroups.findByName(newName).isPresent()) {
            counter++;
            newName = base + " " + counter;
        }

        RegionPlayerGroup newGroup = new RegionPlayerGroup(
                newName,
                Collections.emptySet(),
                new RuleSettings(newName)
        );

        try {
            RegionGroups updated = currentGroups.addGroup(newGroup);
            sendGroupsUpdate(updated);
            selectedGroupName = newName;
        } catch (IllegalArgumentException e) {
            PrivitMod.LOGGER.warn("[RegionGroupsTab] Failed to add new group: {}", e.getMessage());
        }
    }

    private void onGroupSelected(RegionPlayerGroup group) {
        selectedGroupName = group.getName();
        if (group.isVisitorsGroup() && currentSubTab == SubTab.PLAYERS) {
            currentSubTab = SubTab.RULES;
        }
        updateRightContent();
        updateUI();
    }

    private void onPlayerRemove(PlayerIdentity identity) {
        getSelectedGroup().ifPresent(group -> {
            RegionGroups current = controller.getLocalState().groups();

            current.findByName(group.getName()).ifPresent(oldGroup -> {
                try {
                    RegionPlayerGroup updated = new RegionPlayerGroup(oldGroup);
                    updated.removeMember(identity.uuid());
                    sendGroupsUpdate(current.withUpdatedGroup(group.getName(), updated));
                } catch (UnsupportedOperationException e) {
                    PrivitMod.LOGGER.warn("[RegionGroupsTab] Failed to remove player: {}", e.getMessage());
                }
            });
        });
    }

    private void onAddPlayerClicked() {
        getSelectedGroup().ifPresent(group ->
                MinecraftClient.getInstance().setScreen(
                        new AddPlayerScreen(controller, group, this::updateRightContent)
                )
        );
    }

    private void onRuleToggled(Rule rule, boolean newState) {
        getSelectedGroup().ifPresent(group -> {
            RegionGroups current = controller.getLocalState().groups();

            current.findByName(group.getName()).ifPresent(oldGroup -> {
                if (oldGroup.isRuleEnabled(rule) == newState) return;

                RegionPlayerGroup updated = new RegionPlayerGroup(oldGroup);

                updated.setRuleEnabled(rule, newState);
                sendGroupsUpdate(current.withUpdatedGroup(group.getName(), updated));
            });
        });
    }

    private Optional<RegionPlayerGroup> getSelectedGroup() {
        if (selectedGroupName == null) return Optional.empty();
        return controller.getLocalState().groups().findByName(selectedGroupName);
    }

    private void sendGroupsUpdate(RegionGroups updatedGroups) {
        RegionGuiUpdateC2SPacket packet = new RegionGuiUpdateC2SPacket(
                this.controller.getLocalState().id(),
                true,
                MinecraftClient.getInstance().player.getName().getString(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.of(updatedGroups),
                Optional.empty()
        );

        this.controller.sendUpdate(packet);
    }

    private void switchSubTab(SubTab newTab) {
        currentSubTab = newTab;
        updateRightContent();
        updateVisibility();
    }

    private void updateRightContent() {
        Optional<RegionPlayerGroup> groupOpt = getSelectedGroup();
        if (groupOpt.isEmpty()) {
            playerList.visible = false;
            ruleList.visible = false;
            return;
        }

        RegionPlayerGroup group = groupOpt.get();

        playerList.updateEntries(ClientPlayerIdentityCache.getInstance().getIdentities(group.getMembers()));
        playerList.setIsOwnerGroup(group.isOwnerGroup());
        ruleList.refreshEntries(group.getRuleSettings().getActiveRules());

        if (currentSubTab == SubTab.RULES) {
            playerList.visible = false;
            ruleList.visible = true;
        } else {
            ruleList.visible = false;
            playerList.visible = true;
        }

        updateButtonStates();
        updateVisibility();
    }

    private void updateButtonStates() {
        Optional<RegionPlayerGroup> groupOpt = getSelectedGroup();

        if (groupOpt.isEmpty()) {
            subTabRulesButton.active = false;
            subTabPlayersButton.active = false;
            extraTabButton.active = false;
            subTabRulesButton.setTooltip(Tooltip.of(Text.literal("No group selected")));
            subTabPlayersButton.setTooltip(Tooltip.of(Text.literal("No group selected")));
            extraTabButton.setTooltip(Tooltip.of(Text.literal("No group selected")));
            return;
        }

        RegionPlayerGroup group = groupOpt.get();
        boolean isOwner = group.isOwnerGroup();
        boolean isVisitors = group.isVisitorsGroup();
        boolean isSpecial = isOwner || isVisitors;

        extraTabButton.active = !isSpecial;
        extraTabButton.setTooltip(Tooltip.of(
                isSpecial
                        ? Text.translatable("privit.gui.groups.button.edit.disabled")
                        : Text.translatable("privit.gui.groups.button.edit")
        ));

        subTabPlayersButton.active = !isVisitors && (currentSubTab != SubTab.PLAYERS);
        subTabPlayersButton.setTooltip(Tooltip.of(
                isVisitors
                        ? Text.translatable("privit.gui.groups.button.players.disabled")
                        : Text.translatable("privit.gui.groups.button.players")
        ));

        subTabRulesButton.active = (currentSubTab != SubTab.RULES);
        subTabRulesButton.setTooltip(Tooltip.of(Text.translatable("privit.gui.groups.button.rules")));
    }

    @Override
    public List<ClickableWidget> getWidgets() {
        List<ClickableWidget> all = new ArrayList<>(widgets);

        if (playerList != null) all.add(playerList);
        if (groupList != null) all.add(groupList);
        if (ruleList != null) all.add(ruleList);

        return all;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int titleWidth = textRenderer.getWidth(Text.translatable("privit.gui.groups.text.title"));
        int titleX = x + (widthCenter - titleWidth) / 2;

        context.drawTextWithShadow(textRenderer,
                Text.translatable("privit.gui.groups.text.title"),
                titleX, y + 5, 0xFFFFFFFF);
    }

    @Override
    public void renderPendingTooltips(DrawContext context) {
        if (groupList != null) groupList.renderPendingTooltip(context);
        if (playerList != null) playerList.renderPendingTooltip(context);
        if (ruleList != null) ruleList.renderPendingTooltip(context);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        return false;
    }

    @Override
    public void setVisible(boolean visible) {
        this.isTabVisible = visible;
        updateVisibility();
    }

    @Override
    public void updateUI() {
        updateButtonStates();
    }

    @Override
    public void updateVisibility() {
        for (ClickableWidget w : getWidgets()) w.visible = isTabVisible;

        if (isTabVisible) {
            if (playerList != null) playerList.visible = (currentSubTab == SubTab.PLAYERS);
            if (ruleList != null) ruleList.visible = (currentSubTab == SubTab.RULES);
        }
    }

    @Override
    public void updateState() {
        RegionGroups currentGroups = controller.getLocalState().groups();
        List<RegionPlayerGroup> groupsList = currentGroups.getAll();

        groupList.updateEntries(new ArrayList<>(currentGroups.getAll()), selectedGroupName);

        if (selectedGroupName != null) {
            if (currentGroups.findByName(selectedGroupName).isEmpty()) {
                selectedGroupName = groupsList.isEmpty() ? null : groupsList.get(0).getName();
            }
        } else if (!groupsList.isEmpty()) {
            selectedGroupName = groupsList.get(0).getName();
        }

        updateRightContent();
        updateUI();
    }
}