package dev.slarrties.privit.server.tracking.origin;

import net.minecraft.nbt.CompoundTag;

public interface OriginTracker {
    void clearAll();
    void onWorldUnload();
    default void onServerTick() {}
    default boolean isPersistent() { return false; }
    default CompoundTag toNbt() { return new CompoundTag(); }
    default void fromNbt(CompoundTag tag) {}
}