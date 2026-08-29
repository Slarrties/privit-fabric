package dev.slarrties.privit.common.config;

import dev.slarrties.privit.PrivitMod;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;

import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

public final class ConfigManager {

    private static PrivitConfig config;
    private static CommentedFileConfig fileConfig;

    private ConfigManager() {}

    public static void init() {
        Path path = FabricLoader.getInstance()
                .getConfigDir()
                .resolve(PrivitMod.MOD_ID + ".toml");

        fileConfig = CommentedFileConfig.builder(path)
                .sync()
                .autosave()
                .writingMode(WritingMode.REPLACE)
                .build();

        fileConfig.load();

        if (fileConfig.isEmpty()) {
            writeDefaults(fileConfig);
            fileConfig.save();
        }

        config = readFrom(fileConfig);
    }

    public static PrivitConfig get() {
        if (config == null) throw new IllegalStateException("ConfigManager is not initialized! Call ConfigManager.init() first.");

        return config;
    }

    public static void reload() {
        if (fileConfig == null) return;
        fileConfig.load();
        config = readFrom(fileConfig);
    }

    public static void save() {
        if (fileConfig == null || config == null) return;
        writeTo(fileConfig, config);
        fileConfig.save();
    }

    private static PrivitConfig readFrom(CommentedConfig raw) {
        PrivitConfig cfg = new PrivitConfig();
        ConfigSerializer.readSections(raw, cfg.sections());
        return cfg;
    }

    private static void writeTo(CommentedConfig raw, PrivitConfig cfg) {
        ConfigSerializer.writeSections(raw, cfg.sections());
    }

    private static void writeDefaults(CommentedConfig raw) {
        PrivitConfig defaults = new PrivitConfig();
        ConfigSerializer.writeDefaultSections(raw, defaults.sections());
    }
}