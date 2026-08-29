package dev.slarrties.privit.server.region.protection.mixin.use_trial_mechanics;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.common.network.payload.s2c.HudNotificationS2CPacket;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;

import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.RaycastContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.block.enums.TrialSpawnerState;
import net.minecraft.block.spawner.TrialSpawnerLogic;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@AssociatedRule(Rule.USE_TRIAL_MECHANICS)
@Mixin(TrialSpawnerLogic.class)
public abstract class TrialSpawnerLogicProtectionMixin {

    @Unique
    private static final int NOTIFICATION_COOLDOWN = 40;

    @Unique
    private long lastNotificationTick = -1;

    @Inject(
            method = "tickServer",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/block/spawner/TrialSpawnerLogic;setSpawnerState(Lnet/minecraft/world/World;Lnet/minecraft/block/enums/TrialSpawnerState;)V"
            ),
            cancellable = true
    )
    private void checkPermissionBeforeStateChange(ServerWorld world, BlockPos pos, boolean ominous, CallbackInfo ci) {
        if (world.isClient()) return;

        List<ServerPlayerEntity> nearbyPlayers = world.getEntitiesByClass(
                ServerPlayerEntity.class,
                new Box(pos).expand(15),
                player -> true
        );

        boolean hasAuthorizedPlayer = false;
        boolean shouldNotify = false;

        for (ServerPlayerEntity player : nearbyPlayers) {
            boolean hasPermission = RegionPermissionChecker.isAllowed(player, Rule.USE_TRIAL_MECHANICS, pos);
            boolean hasLineOfSight = hasLineOfSightToSpawner(world, pos, player);

            if (hasPermission) {
                hasAuthorizedPlayer = true;
            } else if (hasLineOfSight) {
                shouldNotify = true;
            }
        }

        if (!hasAuthorizedPlayer) {
            forceSafeState((TrialSpawnerLogic) (Object) this, world, pos);
            ci.cancel();

            if (shouldNotify && world.getTime() - lastNotificationTick >= NOTIFICATION_COOLDOWN) {
                sendNotificationsToUnauthorizedPlayers(world, pos, nearbyPlayers);
                lastNotificationTick = world.getTime();
            }
        }
    }

    @Unique
    private boolean hasLineOfSightToSpawner(ServerWorld world, BlockPos spawnerPos, ServerPlayerEntity player) {
        Vec3d spawnerCenter = Vec3d.ofCenter(spawnerPos);
        Vec3d playerEye = player.getEyePos();

        BlockHitResult hitResult = world.raycast(new RaycastContext(
                playerEye,
                spawnerCenter,
                RaycastContext.ShapeType.VISUAL,
                RaycastContext.FluidHandling.NONE,
                player
        ));

        return hitResult.getBlockPos().equals(spawnerPos) || hitResult.getType() == net.minecraft.util.hit.HitResult.Type.MISS;
    }

    @Unique
    private void sendNotificationsToUnauthorizedPlayers(ServerWorld world, BlockPos pos, List<ServerPlayerEntity> players) {
        for (ServerPlayerEntity player : players) {
            if (!RegionPermissionChecker.isAllowed(player, Rule.USE_TRIAL_MECHANICS, pos)) {
                ServerPlayNetworking.send(player, new HudNotificationS2CPacket(
                        NotificationType.DENY_USE_TRIAL_MECHANICS, Color.RED
                ));
            }
        }
    }

    @Unique
    private void forceSafeState(TrialSpawnerLogic logic, ServerWorld world, BlockPos pos) {
        var currentState = logic.getSpawnerState();
        if (currentState == TrialSpawnerState.ACTIVE || currentState == TrialSpawnerState.WAITING_FOR_REWARD_EJECTION) {
            logic.setSpawnerState(world, TrialSpawnerState.WAITING_FOR_PLAYERS);
            logic.updateListeners();
        }
    }
}