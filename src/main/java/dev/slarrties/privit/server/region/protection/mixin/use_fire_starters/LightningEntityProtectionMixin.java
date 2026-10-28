package dev.slarrties.privit.server.region.protection.mixin.use_fire_starters;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;
import dev.slarrties.privit.server.tracking.protection.FireOriginTracker;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LightningBolt;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@AssociatedRule(Rule.USE_FIRE_STARTERS)
@Mixin(LightningBolt.class)
public abstract class LightningEntityProtectionMixin {

    @Inject(method = "spawnFire", at = @At("HEAD"), cancellable = true)
    private void controlLightningFireCreation(int spreadAttempts, CallbackInfo ci) {
        LightningBolt lightning = (LightningBolt) (Object) this;

        if (lightning.level() instanceof ServerLevel serverLevel) {
            UUID responsible = WorldRegistry.get(serverLevel)
                    .getTrackerManager()
                    .getLightningOriginTracker()
                    .getResponsible(lightning);

            if (responsible == null) {
                if (lightning.getCause() != null) {
                    responsible = lightning.getCause().getUUID();
                }
            }
            if (responsible == null) return;

            BlockPos firePos = lightning.blockPosition();
            if (!RegionPermissionChecker.isAllowed(responsible, Rule.USE_FIRE_STARTERS, firePos, serverLevel)) {
                ServerPlayer serverPlayer = serverLevel.getServer().getPlayerList().getPlayer(responsible);
                PlayerNotification.trySend(serverPlayer, NotificationType.DENY_USE_FIRE_STARTER, Color.RED);
                ci.cancel();
            } else {
                FireOriginTracker fireTracker = WorldRegistry.get(serverLevel).getTrackerManager().getFireOriginTracker();
                fireTracker.record(firePos, responsible);
            }
        }
    }
}