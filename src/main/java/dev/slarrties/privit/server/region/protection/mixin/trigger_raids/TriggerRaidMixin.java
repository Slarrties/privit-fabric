package dev.slarrties.privit.server.region.protection.mixin.trigger_raids;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;
import dev.slarrties.privit.server.tracking.protection.RaidOriginTracker;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.PoiTypeTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raids;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.ai.village.poi.PoiManager;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.OptionalInt;

@AssociatedRule(Rule.TRIGGER_RAIDS)
@Mixin(Raids.class)
public abstract class TriggerRaidMixin {

    @Inject(method = "createOrExtendRaid", at = @At("HEAD"), cancellable = true)
    private void preventRaidTrigger(ServerPlayer player, BlockPos pos, CallbackInfoReturnable<Raid> cir) {
        if (!RegionPermissionChecker.isAllowed(player.getUUID(), Rule.TRIGGER_RAIDS, player.blockPosition(), player.level())) {
            denyRaid(player, cir);
            return;
        }

        BlockPos villageCenter = calculateVillageCenter(player.level(), pos);
        if (villageCenter != null && !RegionPermissionChecker.isAllowed(player.getUUID(), Rule.TRIGGER_RAIDS, villageCenter, player.level())) {
            denyRaid(player, cir);
        }
    }

    @Inject(method = "createOrExtendRaid", at = @At("RETURN"))
    private void recordRaidOrigin(ServerPlayer player, BlockPos pos, CallbackInfoReturnable<Raid> cir) {
        Raid raid = cir.getReturnValue();
        if (raid != null) {
            ServerLevel level = player.level();
            OptionalInt raidIdOpt = level.getRaids().getId(raid);
            if (!raidIdOpt.isPresent()) return;
            int raidId = raidIdOpt.getAsInt();

            RaidOriginTracker raidTracker = WorldRegistry.get(level)
                    .getTrackerManager()
                    .getRaidOriginTracker();
            if (raidTracker.getResponsible(raidId) == null) {
                raidTracker.record(raidId, player.getUUID());
            }
        }
    }

    @Unique
    private void denyRaid(ServerPlayer player, CallbackInfoReturnable<Raid> cir) {
        PlayerNotification.trySend(player, NotificationType.DENY_TRIGGER_RAID, Color.RED);
        cir.setReturnValue(null);
    }

    @Unique
    private BlockPos calculateVillageCenter(ServerLevel level, BlockPos pos) {
        List<PoiRecord> pois = level.getPoiManager()
                .getInRange(
                        holder -> holder.is(PoiTypeTags.VILLAGE),
                        pos,
                        64,
                        PoiManager.Occupancy.IS_OCCUPIED
                )
                .toList();
        if (pois.isEmpty()) return null;

        double x = 0, y = 0, z = 0;
        for (PoiRecord poi : pois) {
            BlockPos p = poi.getPos();
            x += p.getX();
            y += p.getY();
            z += p.getZ();
        }
        int count = pois.size();
        return new BlockPos((int)(x / count), (int)(y / count), (int)(z / count));
    }
}