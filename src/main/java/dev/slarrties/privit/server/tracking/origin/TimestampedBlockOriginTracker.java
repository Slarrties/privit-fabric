package dev.slarrties.privit.server.tracking.origin;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public abstract class TimestampedBlockOriginTracker extends AbstractBlockOriginTracker {

    public record ResponsibleTimestamp(UUID owner, long timestamp) {}

    protected final ConcurrentHashMap<Long, ResponsibleTimestamp> records = new ConcurrentHashMap<>();

    protected TimestampedBlockOriginTracker(ServerLevel level) {
        super(level);
    }

    @Override
    public void record(BlockPos pos, UUID playerUuid) {
        if (pos == null || playerUuid == null || level.isClientSide()) return;
        records.put(pos.asLong(), new ResponsibleTimestamp(playerUuid, level.getGameTime()));
        super.record(pos, playerUuid);
    }

    @Override
    public void propagate(BlockPos from, BlockPos to) {
        if (from == null || to == null || level.isClientSide()) return;
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
    public ResponsibleTimestamp getResponsibleTimestamp(BlockPos pos) {
        if (pos == null || level.isClientSide()) return null;
        return records.get(pos.asLong());
    }

    @Override
    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        for (var entry : records.entrySet()) {
            CompoundTag recordTag = new CompoundTag();
            recordTag.putString("owner", entry.getValue().owner().toString());
            recordTag.putLong("time", entry.getValue().timestamp());
            tag.put(Long.toString(entry.getKey()), recordTag);
        }
        return tag;
    }

    @Override
    public void fromNbt(CompoundTag tag) {
        records.clear();
        super.clearAll();
        if (tag == null) return;
        for (String key : tag.keySet()) {
            try {
                long posLong = Long.parseLong(key);
                CompoundTag recordTag = tag.getCompoundOrEmpty(key);

                String ownerStr = recordTag.getStringOr("owner", "");
                if (ownerStr.isEmpty()) continue;

                UUID owner = UUID.fromString(ownerStr);
                long time = recordTag.getLongOr("time", 0L);

                records.put(posLong, new ResponsibleTimestamp(owner, time));
                this.responsible.put(posLong, owner);
            } catch (Exception ignored) {}
        }
    }
}