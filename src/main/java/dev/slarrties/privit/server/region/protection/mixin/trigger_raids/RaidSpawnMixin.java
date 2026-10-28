package dev.slarrties.privit.server.region.protection.mixin.trigger_raids;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;
import dev.slarrties.privit.server.tracking.protection.RaidOriginTracker;
import dev.slarrties.privit.server.tracking.protection.InfluencedEntityTracker;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raider;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;
import java.util.OptionalInt;

@AssociatedRule(Rule.TRIGGER_RAIDS)
@Mixin(Raid.class)
public abstract class RaidSpawnMixin {

    @Shadow
    public abstract boolean isStopped();

    @Inject(method = "joinRaid", at = @At("HEAD"), cancellable = true)
    private void onAddRaiderHead(ServerLevel level, int wave, Raider raider, @Nullable BlockPos pos, boolean exists, CallbackInfo ci) {
        if (exists || pos == null) return;
        UUID triggerUuid = getTriggerUuid(level);
        if (triggerUuid == null) return;

        boolean allowed = RegionPermissionChecker.isAllowed(triggerUuid, Rule.TRIGGER_RAIDS, pos, level);
        if (!allowed) ci.cancel();
    }

    @Inject(method = "joinRaid", at = @At("TAIL"))
    private void onAddRaiderTail(ServerLevel level, int wave, Raider raider, @Nullable BlockPos pos, boolean exists, CallbackInfo ci) {
        if (exists || pos == null) return;
        UUID triggerUuid = getTriggerUuid(level);
        if (triggerUuid == null) return;

        InfluencedEntityTracker influencedTracker = WorldRegistry.get(level)
                .getTrackerManager()
                .getInfluencedEntityTracker();
        influencedTracker.record(raider, triggerUuid);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void onRaidTick(ServerLevel level, CallbackInfo ci) {
        if (this.isStopped()) {
            RaidOriginTracker raidTracker = WorldRegistry.get(level)
                    .getTrackerManager()
                    .getRaidOriginTracker();
            OptionalInt raidIdOpt = getRaidIdOpt(level);
            if (raidIdOpt.isPresent()) {
                raidTracker.remove(raidIdOpt.getAsInt());
            }
        }
    }

    @Unique
    private UUID getTriggerUuid(ServerLevel level) {
        OptionalInt raidIdOpt = getRaidIdOpt(level);
        if (!raidIdOpt.isPresent()) return null;

        return WorldRegistry.get(level)
                .getTrackerManager()
                .getRaidOriginTracker()
                .getResponsible(raidIdOpt.getAsInt());
    }

    @Unique
    private OptionalInt getRaidIdOpt(ServerLevel level) {
        return level.getRaids().getId((Raid) (Object) this);
    }
}