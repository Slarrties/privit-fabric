package dev.slarrties.privit.server.tracking.origin;

import dev.slarrties.privit.PrivitMod;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractBlockOriginTracker implements BlockOriginTracker {

    protected final ServerLevel level;
    protected final ConcurrentHashMap<Long, UUID> responsible = new ConcurrentHashMap<>();

    protected AbstractBlockOriginTracker(ServerLevel level) {
        this.level = level;
    }

    @Override
    public void record(BlockPos pos, UUID playerUuid) {
        if (pos == null || playerUuid == null || level.isClientSide()) return;
        responsible.put(pos.asLong(), playerUuid);
    }

    @Override
    public void propagate(BlockPos from, BlockPos to) {
        if (from == null || to == null || level.isClientSide()) return;
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
    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();

        for (var entry : responsible.entrySet()) {
            tag.putString(Long.toString(entry.getKey()), entry.getValue().toString());
        }

        return tag;
    }

    @Override
    public void fromNbt(CompoundTag tag) {
        if (tag == null) return;

        responsible.clear();
        for (String key : tag.keySet()) {
            try {
                long posLong = Long.parseLong(key);
                String uuidStr = tag.getStringOr(key, "");

                if (!uuidStr.isEmpty()) {
                    UUID uuid = UUID.fromString(uuidStr);
                    responsible.put(posLong, uuid);
                }
            } catch (Exception e) {
                PrivitMod.LOGGER.warn("[{}] Skipping corrupted entry: {}", getClass().getSimpleName(), key);
            }
        }
    }
}