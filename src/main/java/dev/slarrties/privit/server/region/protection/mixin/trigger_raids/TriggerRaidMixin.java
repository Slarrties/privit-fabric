package dev.slarrties.privit.server.region.protection.mixin.trigger_raids;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;
import dev.slarrties.privit.server.tracking.protection.RaidOriginTracker;

import net.minecraft.util.math.BlockPos;
import net.minecraft.village.raid.Raid;
import net.minecraft.village.raid.RaidManager;
import net.minecraft.world.poi.PointOfInterest;
import net.minecraft.world.poi.PointOfInterestStorage;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.registry.tag.PointOfInterestTypeTags;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@AssociatedRule(Rule.TRIGGER_RAIDS)
@Mixin(RaidManager.class)
public abstract class TriggerRaidMixin {

    @Inject(method = "startRaid", at = @At("HEAD"), cancellable = true)
    private void preventRaidTrigger(ServerPlayerEntity player, CallbackInfoReturnable<Raid> cir) {
        if (!RegionPermissionChecker.isAllowed(player, Rule.TRIGGER_RAIDS, player.getBlockPos())) {
            denyRaid(player, cir);
            return;
        }

        BlockPos villageCenter = calculateVillageCenter(player.getServerWorld(), player.getBlockPos());
        if (villageCenter != null && !RegionPermissionChecker.isAllowed(player, Rule.TRIGGER_RAIDS, villageCenter)) {
            denyRaid(player, cir);
        }
    }

    @Inject(method = "startRaid", at = @At("RETURN"))
    private void recordRaidOrigin(ServerPlayerEntity player, CallbackInfoReturnable<Raid> cir) {
        Raid raid = cir.getReturnValue();

        if (raid != null) {
            RaidOriginTracker raidTracker = WorldRegistry.get(player.getServerWorld())
                    .getTrackerManager()
                    .getRaidOriginTracker();
            if (raidTracker.getResponsible(raid.getRaidId()) == null) {
                raidTracker.record(raid.getRaidId(), player.getUuid());
            }
        }
    }

    @Unique
    private void denyRaid(ServerPlayerEntity player, CallbackInfoReturnable<Raid> cir) {
        PlayerNotification.trySend(player, NotificationType.DENY_TRIGGER_RAID, Color.RED);
        cir.setReturnValue(null);
    }

    @Unique
    private BlockPos calculateVillageCenter(ServerWorld world, BlockPos pos) {
        List<PointOfInterest> pois = world.getPointOfInterestStorage()
                .getInCircle(
                        registryEntry -> registryEntry.isIn(PointOfInterestTypeTags.VILLAGE),
                        pos,
                        64,
                        PointOfInterestStorage.OccupationStatus.IS_OCCUPIED
                )
                .toList();
        if (pois.isEmpty()) return null;

        double x = 0, y = 0, z = 0;
        for (PointOfInterest poi : pois) {
            BlockPos p = poi.getPos();
            x += p.getX();
            y += p.getY();
            z += p.getZ();
        }
        int count = pois.size();
        return new BlockPos((int)(x / count), (int)(y / count), (int)(z / count));
    }
}