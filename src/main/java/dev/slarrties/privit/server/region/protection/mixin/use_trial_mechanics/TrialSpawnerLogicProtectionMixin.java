package dev.slarrties.privit.server.region.protection.mixin.use_trial_mechanics;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.common.network.payload.s2c.HudNotificationS2CPacket;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawner;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawnerState;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@AssociatedRule(Rule.USE_TRIAL_MECHANICS)
@Mixin(TrialSpawner.class)
public abstract class TrialSpawnerLogicProtectionMixin {

    @Unique
    private static final int NOTIFICATION_COOLDOWN = 40;

    @Unique
    private long lastNotificationTick = -1;

    @Inject(
            method = "tickServer",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/entity/trialspawner/TrialSpawner;setState(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/entity/trialspawner/TrialSpawnerState;)V"
            ),
            cancellable = true
    )
    private void checkPermissionBeforeStateChange(ServerLevel level, BlockPos pos, boolean ominous, CallbackInfo ci) {
        if (level.isClientSide()) return;

        List<ServerPlayer> nearbyPlayers = level.getEntitiesOfClass(
                ServerPlayer.class,
                new AABB(pos).inflate(15),
                player -> true
        );

        boolean hasAuthorizedPlayer = false;
        boolean shouldNotify = false;

        for (ServerPlayer player : nearbyPlayers) {
            boolean hasPermission = RegionPermissionChecker.isAllowed(player.getUUID(), Rule.USE_TRIAL_MECHANICS, pos, level);
            boolean hasLineOfSight = hasLineOfSightToSpawner(level, pos, player);

            if (hasPermission) {
                hasAuthorizedPlayer = true;
            } else if (hasLineOfSight) {
                shouldNotify = true;
            }
        }

        if (!hasAuthorizedPlayer) {
            forceSafeState((TrialSpawner) (Object) this, level, pos);
            ci.cancel();

            if (shouldNotify && level.getGameTime() - lastNotificationTick >= NOTIFICATION_COOLDOWN) {
                sendNotificationsToUnauthorizedPlayers(level, pos, nearbyPlayers);
                lastNotificationTick = level.getGameTime();
            }
        }
    }

    @Unique
    private boolean hasLineOfSightToSpawner(ServerLevel level, BlockPos spawnerPos, ServerPlayer player) {
        Vec3 spawnerCenter = Vec3.atCenterOf(spawnerPos);
        Vec3 playerEye = player.getEyePosition();
        BlockHitResult hitResult = level.clip(new ClipContext(
                playerEye,
                spawnerCenter,
                ClipContext.Block.VISUAL,
                ClipContext.Fluid.NONE,
                player
        ));

        return hitResult.getBlockPos().equals(spawnerPos) || hitResult.getType() == net.minecraft.world.phys.HitResult.Type.MISS;
    }

    @Unique
    private void sendNotificationsToUnauthorizedPlayers(ServerLevel level, BlockPos pos, List<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            if (!RegionPermissionChecker.isAllowed(player.getUUID(), Rule.USE_TRIAL_MECHANICS, pos, level)) {
                ServerPlayNetworking.send(player, new HudNotificationS2CPacket( // TODO
                        NotificationType.DENY_USE_TRIAL_MECHANICS, Color.RED
                ));
            }
        }
    }

    @Unique
    private void forceSafeState(TrialSpawner spawner, ServerLevel level, BlockPos pos) {
        TrialSpawnerState currentState = spawner.getState();

        if (currentState == TrialSpawnerState.ACTIVE || currentState == TrialSpawnerState.WAITING_FOR_REWARD_EJECTION) {
            spawner.setState(level, TrialSpawnerState.INACTIVE);
            spawner.markUpdated();
        }
    }
}