package dev.slarrties.privit.server.tracking.origin;

import dev.slarrties.privit.PrivitMod;

import net.minecraft.world.entity.Entity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractEntityOriginTracker implements EntityOriginTracker {

    protected final ServerLevel level;
    protected final ConcurrentHashMap<UUID, UUID> responsible = new ConcurrentHashMap<>();

    protected AbstractEntityOriginTracker(ServerLevel level) {
        this.level = level;
    }

    @Override
    public void record(Entity entity, UUID playerUuid) {
        if (entity == null || playerUuid == null || entity.level().isClientSide()) return;
        UUID entityUuid = entity.getUUID();
        UUID currentResponsible = responsible.get(entityUuid);
        if (currentResponsible != null && currentResponsible.equals(playerUuid)) return;
        responsible.put(entityUuid, playerUuid);
    }

    @Override
    public void propagate(Entity from, Entity to) {
        if (from == null || to == null || from.level().isClientSide()) return;
        UUID playerUuid = responsible.get(from.getUUID());
        if (playerUuid != null) {
            responsible.put(to.getUUID(), playerUuid);
        }
    }

    @Override @Nullable
    public UUID getResponsible(Entity entity) {
        if (entity == null || entity.level().isClientSide()) return null;
        return responsible.get(entity.getUUID());
    }

    @Override
    public void remove(Entity entity) {
        if (entity != null) {
            responsible.remove(entity.getUUID());
        }
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
    public void onServerTick() {
        responsible.entrySet().removeIf(entry -> {
            Entity entity = this.level.getEntity(entry.getKey());
            return entity == null || entity.isRemoved();
        });
    }

    @Override
    public boolean isPersistent() {
        return true;
    }

    @Override
    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        for (var entry : responsible.entrySet()) {
            tag.putString(entry.getKey().toString(), entry.getValue().toString());
        }
        return tag;
    }

    @Override
    public void fromNbt(CompoundTag tag) {
        responsible.clear();
        if (tag == null) return;
        for (String key : tag.keySet()) {
            try {
                UUID entityUuid = UUID.fromString(key);
                String uuidStr = tag.getStringOr(key, "");
                if (!uuidStr.isEmpty()) {
                    UUID playerUuid = UUID.fromString(uuidStr);
                    responsible.put(entityUuid, playerUuid);
                }
            } catch (Exception e) {
                PrivitMod.LOGGER.warn("[{}] Skipping corrupted entry: {}", getClass().getSimpleName(), key);
            }
        }
    }
}