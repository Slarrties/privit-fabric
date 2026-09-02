package dev.slarrties.privit.server.tracking.origin;

import net.minecraft.nbt.NbtCompound;

public interface OriginTracker {

    void clearAll();
    void onWorldUnload();
    default void onServerTick() {}
    default boolean isPersistent() { return false; }
    default NbtCompound toNbt() { return new NbtCompound(); }
    default void fromNbt(NbtCompound tag) {}

}