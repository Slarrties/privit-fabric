package dev.slarrties.privit.server.region.protection.mixin.use_fire_starters;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;
import dev.slarrties.privit.server.tracking.protection.FireOriginTracker;

import net.minecraft.util.math.BlockPos;
import net.minecraft.entity.LightningEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@AssociatedRule(Rule.USE_FIRE_STARTERS)
@Mixin(LightningEntity.class)
public abstract class LightningEntityProtectionMixin {

    @Inject(method = "spawnFire", at = @At("HEAD"), cancellable = true)
    private void controlLightningFireCreation(int spreadAttempts, CallbackInfo ci) {
        LightningEntity lightning = (LightningEntity) (Object) this;

        if (lightning.getWorld() instanceof ServerWorld serverWorld) {
            UUID responsible = WorldRegistry.get(serverWorld)
                    .getTrackerManager()
                    .getLightningOriginTracker()
                    .getResponsible(lightning);
            if (responsible == null) {
                if (lightning.getChanneler() != null)
                    responsible = lightning.getChanneler().getUuid();
            }
            if (responsible == null) return;
            BlockPos firePos = lightning.getBlockPos();

            if (!RegionPermissionChecker.isAllowed(responsible, Rule.USE_FIRE_STARTERS, firePos, serverWorld)) {
                ServerPlayerEntity serverPlayer = serverWorld.getServer().getPlayerManager().getPlayer(responsible);
                PlayerNotification.trySend(serverPlayer, NotificationType.DENY_USE_FIRE_STARTER, Color.RED);
                ci.cancel();
            } else {
                FireOriginTracker fireTracker = WorldRegistry.get(serverWorld).getTrackerManager().getFireOriginTracker();
                fireTracker.record(firePos, responsible);
            }
        }
    }
}