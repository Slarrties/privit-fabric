package dev.slarrties.privit.server.region.protection.mixin.trigger_raids;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;
import dev.slarrties.privit.server.tracking.protection.RaidOriginTracker;
import dev.slarrties.privit.server.tracking.protection.InfluencedEntityTracker;

import net.minecraft.util.math.BlockPos;
import net.minecraft.village.raid.Raid;
import net.minecraft.entity.raid.RaiderEntity;
import net.minecraft.server.world.ServerWorld;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@AssociatedRule(Rule.TRIGGER_RAIDS)
@Mixin(Raid.class)
public abstract class RaidSpawnMixin {

    @Shadow @Final private ServerWorld world;

    @Shadow public abstract int getRaidId();

    @Inject(method = "addRaider", at = @At("HEAD"), cancellable = true)
    private void onAddRaiderHead(int wave, RaiderEntity raider, @Nullable BlockPos pos, boolean existing, CallbackInfo ci) {
        if (existing || pos == null) return;

        UUID triggerUuid = getTriggerUuid();
        if (triggerUuid == null) return;

        boolean allowed = RegionPermissionChecker.isAllowed(triggerUuid, Rule.TRIGGER_RAIDS, pos, this.world);
        if (!allowed) ci.cancel();
    }

    @Inject(method = "addRaider", at = @At("TAIL"))
    private void onAddRaiderTail(int wave, RaiderEntity raider, @Nullable BlockPos pos, boolean existing, CallbackInfo ci) {
        if (existing || pos == null) return;

        UUID triggerUuid = getTriggerUuid();
        if (triggerUuid == null) return;

        InfluencedEntityTracker influencedTracker = WorldRegistry.get(this.world)
                .getTrackerManager()
                .getInfluencedEntityTracker();
        influencedTracker.record(raider, triggerUuid);
    }

    @Inject(method = "invalidate", at = @At("HEAD"))
    private void onRaidInvalidate(CallbackInfo ci) {
        RaidOriginTracker raidTracker = WorldRegistry.get(this.world)
                .getTrackerManager()
                .getRaidOriginTracker();
        raidTracker.remove(this.getRaidId());
    }

    @Unique
    private UUID getTriggerUuid() {
        return WorldRegistry.get(this.world)
                .getTrackerManager()
                .getRaidOriginTracker()
                .getResponsible(this.getRaidId());
    }
}