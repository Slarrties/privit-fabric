package dev.slarrties.privit.server.tracking.origin;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

public interface BlockOriginTracker extends OriginTracker {
    void record(BlockPos pos, UUID playerUuid);
    void propagate(BlockPos from, BlockPos to);
    void remove(BlockPos pos);
    @Nullable UUID getResponsible(BlockPos pos);
}