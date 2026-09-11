package dev.slarrties.privit.common.config;

import dev.slarrties.privit.PrivitMod;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;

import net.fabricmc.loader.api.FabricLoader;

import java.util.List;
import java.util.ArrayList;
import java.nio.file.Path;

public final class ConfigManager {

    private static PrivitConfig config;
    private static CommentedFileConfig fileConfig;
    private static final List<ConfigReloadListener> listeners = new ArrayList<>();

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
        loadAndReconcile();
    }

    public static PrivitConfig get() {
        if (config == null) throw new IllegalStateException("ConfigManager is not initialized! Call ConfigManager.init() first.");

        return config;
    }

    public static void reload() {
        if (fileConfig == null) return;
        fileConfig.load();
        loadAndReconcile();
    }

    public static void save() {
        if (fileConfig == null || config == null) return;
        writeTo(fileConfig, config);
        fileConfig.save();
        notifyListeners();
    }

    public static void addListener(ConfigReloadListener listener) {
        if (listener == null || listeners.contains(listener)) return;
        listeners.add(listener);
    }

    public static void removeListener(ConfigReloadListener listener) {
        listeners.remove(listener);
    }

    private static void loadAndReconcile() {
        PrivitConfig loaded = readFrom(fileConfig);
        ConfigSerializer.normalize(loaded);

        if (fileConfig.isEmpty() || !ConfigSerializer.isSchemaCurrent(fileConfig, loaded)) {
            ConfigSerializer.writeDefaultSections(fileConfig, loaded.sections());
            fileConfig.save();
        }

        config = loaded;
        notifyListeners();
    }

    private static void notifyListeners() {
        if (config == null || listeners.isEmpty()) return;

        PrivitConfig current = config;
        for (ConfigReloadListener listener : List.copyOf(listeners)) {
            listener.onConfigReloaded(current);
        }
    }

    private static PrivitConfig readFrom(CommentedConfig raw) {
        PrivitConfig cfg = new PrivitConfig();
        ConfigSerializer.readSections(raw, cfg.sections());
        return cfg;
    }

    private static void writeTo(CommentedConfig raw, PrivitConfig cfg) {
        ConfigSerializer.writeSections(raw, cfg.sections());
    }
}