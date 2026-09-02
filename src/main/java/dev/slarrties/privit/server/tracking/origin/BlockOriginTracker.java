package dev.slarrties.privit.server.tracking.origin;

import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public interface BlockOriginTracker extends OriginTracker {

    void record(BlockPos pos, UUID playerUuid);
    void propagate(BlockPos from, BlockPos to);
    void remove(BlockPos pos);
    @Nullable UUID getResponsible(BlockPos pos);

}