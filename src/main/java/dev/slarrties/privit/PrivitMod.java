package dev.slarrties.privit;

import dev.slarrties.privit.common.config.ConfigManager;
import dev.slarrties.privit.common.region.rule.FrozenRules;
import dev.slarrties.privit.common.registry.BlocksRegistry;
import dev.slarrties.privit.common.registry.BlockEntityRegistry;
import dev.slarrties.privit.common.registry.CreativeTabsRegistry;
import dev.slarrties.privit.common.registry.PayloadRegistry;
import dev.slarrties.privit.server.PrivitServer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;
import net.fabricmc.api.ModInitializer;

public class PrivitMod implements ModInitializer {
	public static final String MOD_ID = "privit";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static Identifier id(String path) { return Identifier.of(MOD_ID, path); }

	public static void debug(String msg, Object... args) {
		LOGGER.info("[DEBUG] " + msg, args);
	}

	@Override
	public void onInitialize() {
		ConfigManager.init();
		FrozenRules.load();
		BlocksRegistry.register();
		PayloadRegistry.register();
		BlockEntityRegistry.register();
		CreativeTabsRegistry.register();

		if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
			new PrivitServer().onInitializeServer();
		}
	}
}