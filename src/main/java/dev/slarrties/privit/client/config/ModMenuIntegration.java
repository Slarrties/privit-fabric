package dev.slarrties.privit.client.config;

import com.terraformersmc.modmenu.api.ModMenuApi;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;

@Environment(EnvType.CLIENT)
public final class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        if (isClothConfigLoaded()) {
            return new ClothConfigScreenFactory();
        }
        return MissingClothConfigScreen::new;
    }

    static boolean isClothConfigLoaded() {
        FabricLoader loader = FabricLoader.getInstance();
        return loader.isModLoaded("cloth-config");
    }
}