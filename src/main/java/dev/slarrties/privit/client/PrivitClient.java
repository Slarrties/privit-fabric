package dev.slarrties.privit.client;

import dev.slarrties.privit.PrivitMod;
import dev.slarrties.privit.client.hud.NotificationHudOverlay;
import dev.slarrties.privit.client.hud.RegionNameHudOverlay;
import dev.slarrties.privit.client.network.ClientPacketHandler;
import dev.slarrties.privit.client.render.RegionRenderManager;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

public class PrivitClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientPacketHandler.register();
        RegionRenderManager.register();
        RegionNameHudOverlay.register();
        HudRenderCallback.EVENT.register(new NotificationHudOverlay());
    }
}