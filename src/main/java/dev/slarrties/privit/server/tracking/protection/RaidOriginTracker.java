package dev.slarrties.privit.server.tracking.protection;

import dev.slarrties.privit.server.tracking.origin.OriginTracker;

import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;

import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.HashMap;

public class RaidOriginTracker implements OriginTracker {

    private final ServerLevel level;
    private final Map<Integer, UUID> raidOrigins = new HashMap<>();

    public RaidOriginTracker(ServerLevel level) {
        this.level = level;
    }

    public void record(int raidId, UUID playerUuid) {
        raidOrigins.put(raidId, playerUuid);
    }

    @Nullable
    public UUID getResponsible(int raidId) {
        return raidOrigins.get(raidId);
    }

    public void remove(int raidId) {
        raidOrigins.remove(raidId);
    }

    @Override
    public void clearAll() {
        raidOrigins.clear();
    }

    @Override
    public void onWorldUnload() {}

    @Override
    public void onServerTick() {}

    @Override
    public boolean isPersistent() {
        return true;
    }

    @Override
    public CompoundTag toNbt() {
        CompoundTag root = new CompoundTag();
        ListTag list = new ListTag();

        for (Map.Entry<Integer, UUID> entry : raidOrigins.entrySet()) {
            CompoundTag entryNbt = new CompoundTag();
            entryNbt.putInt("RaidId", entry.getKey());
            entryNbt.putString("PlayerId", entry.getValue().toString());
            list.add(entryNbt);
        }

        root.put("Raids", list);
        return root;
    }

    @Override
    public void fromNbt(CompoundTag nbt) {
        raidOrigins.clear();
        ListTag list = nbt.getListOrEmpty("Raids");

        for (int i = 0; i < list.size(); i++) {
            CompoundTag entryNbt = list.getCompound(i).orElseThrow();
            int raidId = entryNbt.getIntOr("RaidId", -1);
            String playerIdStr = entryNbt.getStringOr("PlayerId", "");

            if (raidId != -1 && !playerIdStr.isEmpty()) {
                try {
                    UUID playerId = UUID.fromString(playerIdStr);
                    raidOrigins.put(raidId, playerId);
                } catch (IllegalArgumentException ignored) {}
            }
        }
    }
}