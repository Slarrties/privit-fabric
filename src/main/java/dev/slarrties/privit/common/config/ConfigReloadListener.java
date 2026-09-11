package dev.slarrties.privit.common.config;

@FunctionalInterface
public interface ConfigReloadListener {
    void onConfigReloaded(PrivitConfig config);
}