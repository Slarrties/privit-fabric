package dev.slarrties.privit.common.registry;

import dev.slarrties.privit.common.network.payload.c2s.*;
import dev.slarrties.privit.common.network.payload.s2c.*;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public final class PayloadRegistry {

    public static void register() {
        // S2C packets (server → client)
        PayloadTypeRegistry.playS2C().register(RegionGuiInitS2CPacket.ID, RegionGuiInitS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(RegionHudInfoS2CPacket.ID, RegionHudInfoS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(RegionGuiUpdateS2CPacket.ID, RegionGuiUpdateS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(RegionGuiCloseS2CPacket.ID, RegionGuiCloseS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(HudNotificationS2CPacket.ID, HudNotificationS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(PlayerNamesS2CPacket.ID, PlayerNamesS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(PlayerSearchResultS2CPacket.ID, PlayerSearchResultS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(RegionGridClearS2CPacket.ID, RegionGridClearS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(RegionGridStateS2CPacket.ID, RegionGridStateS2CPacket.CODEC);

        // C2S packets (client → server)
        PayloadTypeRegistry.playC2S().register(RegionCreateC2SPacket.ID, RegionCreateC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(RegionUpdateC2SPacket.ID, RegionUpdateC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(RegionDeleteC2SPacket.ID, RegionDeleteC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(RegionGuiRequestC2SPacket.ID, RegionGuiRequestC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(RegionGuiUpdateC2SPacket.ID, RegionGuiUpdateC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(RegionGuiCancelC2SPacket.ID, RegionGuiCancelC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(RequestPlayerNamesC2SPacket.ID, RequestPlayerNamesC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(SearchPlayersRequestC2SPacket.ID, SearchPlayersRequestC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(RegionGridStateC2SPacket.ID, RegionGridStateC2SPacket.CODEC);
    }

    private PayloadRegistry() {}
}