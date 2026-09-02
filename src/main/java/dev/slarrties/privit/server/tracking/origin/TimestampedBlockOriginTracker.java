package dev.slarrties.privit.server.tracking.origin;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.server.world.ServerWorld;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public abstract class TimestampedBlockOriginTracker extends AbstractBlockOriginTracker {

    public record ResponsibleTimestamp(UUID owner, long timestamp) {}

    protected final ConcurrentHashMap<Long, ResponsibleTimestamp> records = new ConcurrentHashMap<>();

    protected TimestampedBlockOriginTracker(ServerWorld world) {
        super(world);
    }

    @Override
    public void record(BlockPos pos, UUID playerUuid) {
        if (pos == null || playerUuid == null || world.isClient) return;

        records.put(pos.asLong(), new ResponsibleTimestamp(playerUuid, world.getTime()));
        super.record(pos, playerUuid);
    }

    @Override
    public void propagate(BlockPos from, BlockPos to) {
        if (from == null || to == null || world.isClient) return;

        ResponsibleTimestamp record = records.get(from.asLong());
        if (record == null) return;

        records.put(to.asLong(), record);
        super.record(to, record.owner());
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

    @Nullable
    public TimestampedBlockOriginTracker.ResponsibleTimestamp getResponsibleTimestamp(BlockPos pos) {
        if (pos == null || world.isClient) return null;
        return records.get(pos.asLong());
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
                records.put(posLong, new ResponsibleTimestamp(owner, time));
                this.responsible.put(posLong, owner);
            } catch (Exception ignored) {}
        }
    }
}