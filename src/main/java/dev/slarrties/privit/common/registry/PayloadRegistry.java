package dev.slarrties.privit.common.registry;

import dev.slarrties.privit.common.network.payload.c2s.*;
import dev.slarrties.privit.common.network.payload.s2c.*;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public final class PayloadRegistry {
    public static void register() {
        // S2C packets (server → client)
        PayloadTypeRegistry.clientboundPlay().register(RegionGuiInitS2CPacket.TYPE, RegionGuiInitS2CPacket.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(RegionHudInfoS2CPacket.TYPE, RegionHudInfoS2CPacket.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(RegionGuiUpdateS2CPacket.TYPE, RegionGuiUpdateS2CPacket.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(RegionGuiCloseS2CPacket.TYPE, RegionGuiCloseS2CPacket.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(HudNotificationS2CPacket.TYPE, HudNotificationS2CPacket.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(PlayerNamesS2CPacket.TYPE, PlayerNamesS2CPacket.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(PlayerSearchResultS2CPacket.TYPE, PlayerSearchResultS2CPacket.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(RegionGridClearS2CPacket.TYPE, RegionGridClearS2CPacket.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(RegionGridStateS2CPacket.TYPE, RegionGridStateS2CPacket.CODEC);

        // C2S packets (client → server)
        PayloadTypeRegistry.serverboundPlay().register(RegionCreateC2SPacket.TYPE, RegionCreateC2SPacket.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(RegionUpdateC2SPacket.TYPE, RegionUpdateC2SPacket.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(RegionDeleteC2SPacket.TYPE, RegionDeleteC2SPacket.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(RegionGuiRequestC2SPacket.TYPE, RegionGuiRequestC2SPacket.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(RegionGuiUpdateC2SPacket.TYPE, RegionGuiUpdateC2SPacket.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(RegionGuiCancelC2SPacket.TYPE, RegionGuiCancelC2SPacket.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(RequestPlayerNamesC2SPacket.TYPE, RequestPlayerNamesC2SPacket.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(SearchPlayersRequestC2SPacket.TYPE, SearchPlayersRequestC2SPacket.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(RegionGridStateC2SPacket.TYPE, RegionGridStateC2SPacket.CODEC);
    }

    private PayloadRegistry() {}
}