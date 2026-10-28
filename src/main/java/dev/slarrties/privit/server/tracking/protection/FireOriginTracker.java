package dev.slarrties.privit.server.tracking.protection;

import dev.slarrties.privit.server.tracking.origin.TimestampedBlockOriginTracker;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public final class FireOriginTracker extends TimestampedBlockOriginTracker {

    public FireOriginTracker(ServerLevel level) { super(level); }

    // TODO
//    @Override
//    public void propagate(BlockPos from, BlockPos to) {
//        if (from == null || to == null || level.isClientSide() || from.equals(to)) return;
//
//        ResponsibleTimestamp fromRecord = records.get(from.asLong());
//        if (fromRecord != null) {
//            records.put(to.asLong(), fromRecord);
//            this.responsible.put(to.asLong(), fromRecord.owner());
//        }
//    }
}