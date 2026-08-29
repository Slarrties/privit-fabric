package dev.slarrties.privit.client.gui;

import dev.slarrties.privit.common.network.payload.c2s.RegionGuiUpdateC2SPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import java.util.function.Consumer;

public class RegionGuiController {

    private RegionGuiLocalState localState;
    private final Consumer<RegionGuiUpdateC2SPacket> updateSender;
    private int updateDepth = 0;

    public RegionGuiController(RegionGuiLocalState initialState) {
        this.localState = initialState;
        this.updateSender = ClientPlayNetworking::send;
    }

    public RegionGuiLocalState getLocalState() {
        return localState;
    }

    public void applyServerUpdate(RegionGuiLocalState newState, Runnable uiRefresh) {
        beginServerUpdate(newState);
        try {
            uiRefresh.run();
        } finally {
            endServerUpdate();
        }
    }

    public void suppressDuringRefresh(Runnable refreshAction) {
        beginServerUpdate(localState);
        try {
            refreshAction.run();
        } finally {
            endServerUpdate();
        }
    }

    private void beginServerUpdate(RegionGuiLocalState newState) {
        updateDepth++;
        this.localState = newState;
    }

    private void endServerUpdate() {
        updateDepth--;
        if (updateDepth < 0) updateDepth = 0;
    }

    public boolean isServerDrivenUpdate() {
        return updateDepth > 0;
    }

    public void sendUpdate(RegionGuiUpdateC2SPacket packet) {
        if (isServerDrivenUpdate()) return;

        updateSender.accept(packet);
    }
}