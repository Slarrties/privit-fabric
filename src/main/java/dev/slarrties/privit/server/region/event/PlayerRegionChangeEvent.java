package dev.slarrties.privit.server.region.event;

import dev.slarrties.privit.server.region.Region;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerPlayer;

import org.jetbrains.annotations.Nullable;

public final class PlayerRegionChangeEvent {

    public static final Event<Changed> CHANGED = EventFactory.createArrayBacked(
            Changed.class,
            callbacks -> (player, from, to) -> {
                for (Changed callback : callbacks) {
                    callback.onRegionChanged(player, from, to);
                }
            }
    );

    @FunctionalInterface
    public interface Changed {
        void onRegionChanged(ServerPlayer player,
                             @Nullable Region from,
                             @Nullable Region to);
    }

    private PlayerRegionChangeEvent() {}
}