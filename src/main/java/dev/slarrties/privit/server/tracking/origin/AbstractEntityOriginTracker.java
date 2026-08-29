package dev.slarrties.privit.server.tracking.origin;

import dev.slarrties.privit.PrivitMod;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractEntityOriginTracker implements EntityOriginTracker {

    protected final ServerWorld world;
    protected final ConcurrentHashMap<UUID, UUID> responsible = new ConcurrentHashMap<>();

    protected AbstractEntityOriginTracker(ServerWorld world) {
        this.world = world;
    }

    @Override
    public void record(Entity entity, UUID playerUuid) {
        if (entity == null || playerUuid == null || entity.getWorld().isClient) return;

        UUID entityUuid = entity.getUuid();
        UUID currentResponsible = responsible.get(entityUuid);
        if (currentResponsible != null && currentResponsible.equals(playerUuid)) return;

        responsible.put(entityUuid, playerUuid);
    }

    @Override
    public void propagate(Entity from, Entity to) {
        if (from == null || to == null || from.getWorld().isClient) return;
        UUID playerUuid = responsible.get(from.getUuid());
        if (playerUuid != null) {
            responsible.put(to.getUuid(), playerUuid);
        }
    }

    @Override
    @Nullable
    public UUID getResponsible(Entity entity) {
        if (entity == null || entity.getWorld().isClient) return null;

        return responsible.get(entity.getUuid());
    }

    @Override
    public void remove(Entity entity) {
        if (entity != null) {
            responsible.remove(entity.getUuid());
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
    public void onServerTick(ServerWorld world) {
        responsible.entrySet().removeIf(entry -> {
            Entity entity = world.getEntity(entry.getKey());
            return entity == null || entity.isRemoved();
        });
    }

    @Override
    public boolean isPersistent() {
        return true;
    }

    @Override
    public NbtCompound toNbt() {
        NbtCompound tag = new NbtCompound();
        for (var entry : responsible.entrySet()) {
            tag.putUuid(entry.getKey().toString(), entry.getValue());
        }
        return tag;
    }

    @Override
    public void fromNbt(NbtCompound tag) {
        responsible.clear();
        if (tag == null) return;

        for (String key : tag.getKeys()) {
            try {
                UUID entityUuid = UUID.fromString(key);
                UUID playerUuid = tag.getUuid(key);
                responsible.put(entityUuid, playerUuid);
            } catch (Exception e) {
                PrivitMod.LOGGER.warn("[{}] Skipping corrupted entry: {}", getClass().getSimpleName(), key);
            }
        }
    }
}