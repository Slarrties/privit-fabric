package dev.slarrties.privit.client.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;

import dev.slarrties.privit.client.util.FormattedTooltipText;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.config.PrivitConfig;
import dev.slarrties.privit.common.config.ConfigManager;
import dev.slarrties.privit.common.config.sections.HudSection;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.Screen;

import java.util.Set;
import java.util.Locale;
import java.util.ArrayList;
import java.util.LinkedHashSet;

@Environment(EnvType.CLIENT)
public final class ClothConfigScreenFactory implements ConfigScreenFactory<Screen> {

    @Override
    public Screen create(Screen parent) {
        PrivitConfig config = ConfigManager.get();

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("privit.config.title"))
                .setSavingRunnable(ConfigManager::save);

        ConfigEntryBuilder entries = builder.entryBuilder();

        addHud(builder, entries, config);
        addLimits(builder, entries, config);
        addDefaultRules(builder, entries, config);
        addFrozenRules(builder, entries, config);

        return builder.build();
    }

    private static void addHud(ConfigBuilder builder, ConfigEntryBuilder entries, PrivitConfig config) {
        ConfigCategory hud = builder.getOrCreateCategory(Component.translatable("privit.config.category.hud"));

        hud.addEntry(entries.startEnumSelector(
                        Component.translatable("privit.config.hud.region_name_position"),
                        HudSection.Position.class,
                        config.hud.regionNamePosition
                )
                .setDefaultValue(HudSection.Position.TOP_RIGHT)
                .setTooltip(FormattedTooltipText.of(Component.translatable("privit.config.hud.region_name_position.tooltip")).text().toArray(new Component[0]))
                .setSaveConsumer((HudSection.Position value) -> config.hud.regionNamePosition = value)
                .build());
    }

    private static void addLimits(ConfigBuilder builder, ConfigEntryBuilder entries, PrivitConfig config) {
        ConfigCategory limits = builder.getOrCreateCategory(Component.translatable("privit.config.category.region_limits"));

        limits.addEntry(entries.startIntField(
                        Component.translatable("privit.config.limits.max_area"),
                        config.regionLimits.maxArea
                )
                .setDefaultValue(128 * 64 * 128)
                .setTooltip(FormattedTooltipText.of(Component.translatable("privit.config.limits.max_area.tooltip")).text().toArray(new Component[0]))
                .setSaveConsumer((Integer value) -> config.regionLimits.maxArea = value)
                .build());

        limits.addEntry(entries.startIntField(
                        Component.translatable("privit.config.limits.max_regions_per_player"),
                        config.regionLimits.maxRegionsPerPlayer
                )
                .setDefaultValue(5)
                .setTooltip(FormattedTooltipText.of(Component.translatable("privit.config.limits.max_regions_per_player.tooltip")).text().toArray(new Component[0]))
                .setSaveConsumer((Integer value) -> config.regionLimits.maxRegionsPerPlayer = value)
                .build());
    }

    private static void addDefaultRules(ConfigBuilder builder, ConfigEntryBuilder entries, PrivitConfig config) {
        ConfigCategory category = builder.getOrCreateCategory(Component.translatable("privit.config.category.default_rules"));

        for (Rule rule : Rule.values()) {
            if (rule == Rule.MANAGE) continue;

            String key = rule.name();
            boolean current = Boolean.TRUE.equals(config.defaultRules.rules.get(key));

            category.addEntry(entries.startBooleanToggle(ruleName(key), current)
                    .setDefaultValue(false)
                    .setTooltip(FormattedTooltipText.of(Component.translatable("privit.config.default_rules.tooltip")).text().toArray(new Component[0]))
                    .setSaveConsumer((Boolean value) -> config.defaultRules.rules.put(key, value))
                    .build());
        }
    }

    private static void addFrozenRules(ConfigBuilder builder, ConfigEntryBuilder entries, PrivitConfig config) {
        ConfigCategory category = builder.getOrCreateCategory(Component.translatable("privit.config.category.frozen_rules"));

        Set<String> frozen = new LinkedHashSet<>();
        for (String name : config.frozenRules.disabled) {
            if (name != null && !name.isBlank()) {
                frozen.add(name.trim().toUpperCase(Locale.ROOT));
            }
        }

        for (Rule rule : Rule.values()) {
            if (rule == Rule.MANAGE) continue;

            String key = rule.name();
            boolean current = frozen.contains(key);

            category.addEntry(entries.startBooleanToggle(ruleName(key), current)
                    .setDefaultValue(false)
                    .setTooltip(FormattedTooltipText.of(Component.translatable("privit.config.frozen_rules.tooltip")).text().toArray(new Component[0]))
                    .setSaveConsumer((Boolean value) -> {
                        if (value) {
                            frozen.add(key);
                        } else {
                            frozen.remove(key);
                        }
                        config.frozenRules.disabled = new ArrayList<>(frozen);
                    })
                    .build());
        }
    }

    private static Component ruleName(String key) {
        return Component.translatable("privit.config.rule." + key.toLowerCase(Locale.ROOT));
    }
}