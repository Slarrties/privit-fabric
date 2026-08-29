package dev.slarrties.privit.server.tracking.origin;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public interface BlockOriginTracker extends OriginTracker {

    void record(BlockPos pos, UUID playerUuid);
    void propagate(BlockPos from, BlockPos to);
    void remove(BlockPos pos);
    @Nullable UUID getResponsible(ServerWorld world, BlockPos pos);

}