package dev.slarrties.privit.server.util;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.server.region.event.PlayerRegionChangeEvent;
import dev.slarrties.privit.common.network.payload.s2c.RegionHudInfoS2CPacket;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import java.util.Optional;

public final class RegionHudService {

    private RegionHudService() {}

    public static void init() {
        PlayerRegionChangeEvent.CHANGED.register((player, oldRegion, newRegion) -> {
            Optional<String> name = Optional.ofNullable(newRegion != null ? newRegion.name() : null);
            Optional<Color> color = Optional.ofNullable(newRegion != null ? newRegion.color() : null);

            ServerPlayNetworking.send(player, new RegionHudInfoS2CPacket(name, color));
        });
    }
}