package dev.slarrties.privit.server.tracking.origin;

import dev.slarrties.privit.PrivitMod;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.server.world.ServerWorld;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractBlockOriginTracker implements BlockOriginTracker {

    protected final ServerWorld world;
    protected final ConcurrentHashMap<Long, UUID> responsible = new ConcurrentHashMap<>();

    protected AbstractBlockOriginTracker(ServerWorld world) {
        this.world = world;
    }

    @Override
    public void record(BlockPos pos, UUID playerUuid) {
        if (pos == null || playerUuid == null || world.isClient) return;

        responsible.put(pos.asLong(), playerUuid);
    }

    @Override
    public void propagate(BlockPos from, BlockPos to) {
        if (from == null || to == null || world.isClient) return;

        UUID uuid = responsible.get(from.asLong());

        if (uuid != null) {
            responsible.put(to.asLong(), uuid);
        }
    }

    @Override
    @Nullable
    public UUID getResponsible(BlockPos pos) {
        return responsible.get(pos.asLong());
    }

    @Override
    public void remove(BlockPos pos) {
        if (pos != null) responsible.remove(pos.asLong());
    }

    @Override
    public void clearAll() {
        responsible.clear();
    }

    @Override
    public void onWorldUnload() {
        clearAll();
    }

    @Override
    public void onServerTick() {} // TODO: perform cleanup by reconciling the pos and the block that exists on it

    @Override
    public boolean isPersistent() {
        return true;
    }

    @Override
    public NbtCompound toNbt() {
        NbtCompound tag = new NbtCompound();
        for (var entry : responsible.entrySet()) {
            tag.putUuid(Long.toString(entry.getKey()), entry.getValue());
        }
        return tag;
    }

    @Override
    public void fromNbt(NbtCompound tag) {
        responsible.clear();
        if (tag == null) return;

        for (String key : tag.getKeys()) {
            try {
                long posLong = Long.parseLong(key);
                UUID uuid = tag.getUuid(key);
                responsible.put(posLong, uuid);
            } catch (Exception e) {
                PrivitMod.LOGGER.warn("[{}] Skipping corrupted entry: {}", getClass().getSimpleName(), key);
            }
        }
    }
}