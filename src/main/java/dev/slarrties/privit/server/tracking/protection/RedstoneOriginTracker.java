package dev.slarrties.privit.server.tracking.protection;

import dev.slarrties.privit.server.tracking.origin.AbstractBlockOriginTracker;

import net.minecraft.util.math.BlockPos;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class RedstoneOriginTracker extends AbstractBlockOriginTracker {

    public RedstoneOriginTracker(ServerWorld world) {
        super(world);
    }

    @Override
    public void record(BlockPos pos, UUID playerUuid) {
        super.record(pos, playerUuid);
    }

    @Nullable
    public UUID getResponsible(BlockPos pos) {
        return super.getResponsible(world, pos);
    }

    public void remove(BlockPos pos) {
        super.remove(pos);
    }
}