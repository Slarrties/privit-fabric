package dev.slarrties.privit.server.tracking.origin;

import dev.slarrties.privit.PrivitMod;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public abstract class TimestampedBlockOriginTracker extends AbstractBlockOriginTracker {

    public record OwnershipRecord(UUID owner, long timestamp) {}

    protected final ConcurrentHashMap<Long, OwnershipRecord> records = new ConcurrentHashMap<>();

    protected TimestampedBlockOriginTracker(ServerWorld world) {
        super(world);
    }

    public void record(BlockPos pos, UUID playerUuid) {
        if (pos == null || playerUuid == null || world.isClient) return;

        long time = world.getTime();
        records.put(pos.asLong(), new OwnershipRecord(playerUuid, time));

        super.record(pos, playerUuid);
    }

    @Nullable
    public OwnershipRecord getRecord(BlockPos pos) {
        if (pos == null || world.isClient) return null;
        return records.get(pos.asLong());
    }

    @Nullable
    public UUID getOwner(BlockPos pos) {
        OwnershipRecord record = getRecord(pos);
        return record != null ? record.owner() : null;
    }

    public long getTimestamp(BlockPos pos) {
        OwnershipRecord record = getRecord(pos);
        return record != null ? record.timestamp() : 0L;
    }

    @Override
    public void remove(BlockPos pos) {
        if (pos == null) return;
        records.remove(pos.asLong());
        super.remove(pos);
    }

    @Override
    public void clearAll() {
        records.clear();
        super.clearAll();
    }

    @Override
    public void onWorldUnload() {
        clearAll();
    }

    @Override
    public NbtCompound toNbt() {
        NbtCompound tag = new NbtCompound();
        for (var entry : records.entrySet()) {
            NbtCompound recordTag = new NbtCompound();
            recordTag.putUuid("owner", entry.getValue().owner());
            recordTag.putLong("time", entry.getValue().timestamp());
            tag.put(Long.toString(entry.getKey()), recordTag);
        }
        return tag;
    }

    @Override
    public void fromNbt(NbtCompound tag) {
        records.clear();
        super.clearAll();

        if (tag == null) return;

        for (String key : tag.getKeys()) {
            try {
                long posLong = Long.parseLong(key);
                NbtCompound recordTag = tag.getCompound(key);
                if (!recordTag.containsUuid("owner")) continue;

                UUID owner = recordTag.getUuid("owner");
                long time = recordTag.contains("time") ? recordTag.getLong("time") : 0L;

                records.put(posLong, new OwnershipRecord(owner, time));
                this.responsible.put(posLong, owner);
            } catch (Exception e) {
                PrivitMod.LOGGER.warn("[{}] Skipping corrupted entry: {}", getClass().getSimpleName(), key);
            }
        }
    }
}