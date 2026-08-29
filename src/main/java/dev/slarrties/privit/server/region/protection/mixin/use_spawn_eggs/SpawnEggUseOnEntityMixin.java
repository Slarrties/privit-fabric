package dev.slarrties.privit.server.region.protection.mixin.use_spawn_eggs;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.tracking.protection.InfluencedEntityTracker;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;

import net.minecraft.item.ItemStack;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@AssociatedRule(Rule.USE_SPAWN_EGGS)
@Mixin(SpawnEggItem.class)
public abstract class SpawnEggUseOnEntityMixin {

    @Inject(method = "spawnBaby", at = @At("HEAD"), cancellable = true)
    private void preventSpawnEggOnProtectedEntity(
            PlayerEntity user,
            MobEntity entity,
            EntityType<? extends MobEntity> entityType,
            ServerWorld world,
            Vec3d pos, ItemStack stack,
            CallbackInfoReturnable<Optional<MobEntity>> cir
    ) {
        if (!(user instanceof ServerPlayerEntity serverPlayer)) return;

        BlockPos spawnPos = BlockPos.ofFloored(pos);
        boolean allowed = RegionPermissionChecker.isAllowed(serverPlayer, Rule.USE_SPAWN_EGGS, spawnPos);

        if (!allowed) {
            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_USE_SPAWN_EGG, Color.RED);
            cir.setReturnValue(java.util.Optional.empty());
        }
    }

    @Inject(method = "spawnBaby", at = @At("RETURN"))
    private void recordSpawnedEntity(
            PlayerEntity user,
            MobEntity entity,
            EntityType<? extends MobEntity> entityType,
            ServerWorld world,
            Vec3d pos, ItemStack stack,
            CallbackInfoReturnable<Optional<MobEntity>> cir
    ) {
        if (!(user instanceof ServerPlayerEntity player)) return;

        Optional<MobEntity> result = cir.getReturnValue();
        if (result.isEmpty()) return;

        MobEntity spawned = result.get();
        InfluencedEntityTracker tracker = WorldRegistry.get(world)
                .getTrackerManager()
                .getInfluencedEntityTracker();
        tracker.record(spawned, player.getUuid());
    }
}