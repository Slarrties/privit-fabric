package dev.slarrties.privit.server.tracking.protection;

import dev.slarrties.privit.server.tracking.origin.OriginTracker;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RaidOriginTracker implements OriginTracker {

    private final ServerWorld world;
    private final Map<Integer, UUID> raidOrigins = new HashMap<>();

    public RaidOriginTracker(ServerWorld world) {
        this.world = world;
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
    public NbtCompound toNbt() {
        NbtCompound root = new NbtCompound();
        NbtList list = new NbtList();

        for (Map.Entry<Integer, UUID> entry : raidOrigins.entrySet()) {
            NbtCompound entryNbt = new NbtCompound();
            entryNbt.putInt("RaidId", entry.getKey());
            entryNbt.putUuid("PlayerId", entry.getValue());
            list.add(entryNbt);
        }

        root.put("Raids", list);
        return root;
    }

    @Override
    public void fromNbt(NbtCompound nbt) {
        raidOrigins.clear();
        if (!nbt.contains("Raids", NbtElement.LIST_TYPE)) return;

        NbtList list = nbt.getList("Raids", NbtElement.COMPOUND_TYPE);
        for (NbtElement element : list) {
            NbtCompound entryNbt = (NbtCompound) element;
            int raidId = entryNbt.getInt("RaidId");
            UUID playerId = entryNbt.getUuid("PlayerId");
            raidOrigins.put(raidId, playerId);
        }
    }
}