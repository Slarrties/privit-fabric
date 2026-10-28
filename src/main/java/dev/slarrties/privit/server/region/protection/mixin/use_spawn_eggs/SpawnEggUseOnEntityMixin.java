package dev.slarrties.privit.server.region.protection.mixin.use_spawn_eggs;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;
import dev.slarrties.privit.server.tracking.protection.InfluencedEntityTracker;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@AssociatedRule(Rule.USE_SPAWN_EGGS)
@Mixin(SpawnEggItem.class)
public abstract class SpawnEggUseOnEntityMixin {

    @Inject(method = "spawnOffspringFromSpawnEgg", at = @At("HEAD"), cancellable = true)
    private static void preventSpawnEggOnProtectedEntity(
            Player player,
            Mob parent,
            EntityType<? extends Mob> type,
            ServerLevel level,
            Vec3 pos, ItemStack spawnEggStack,
            CallbackInfoReturnable<Optional<Mob>> cir
    ) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        BlockPos spawnPos = BlockPos.containing(pos);
        boolean allowed = RegionPermissionChecker.isAllowed(serverPlayer.getUUID(), Rule.USE_SPAWN_EGGS, spawnPos, serverPlayer.level());
        if (!allowed) {
            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_USE_SPAWN_EGG, Color.RED);
            cir.setReturnValue(Optional.empty());
        }
    }

    @Inject(method = "spawnOffspringFromSpawnEgg", at = @At("RETURN"))
    private static void recordSpawnedEntity(
            Player player,
            Mob parent,
            EntityType<? extends Mob> type,
            ServerLevel level,
            Vec3 pos, ItemStack spawnEggStack,
            CallbackInfoReturnable<Optional<Mob>> cir
    ) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        Optional<Mob> result = cir.getReturnValue();
        if (result.isEmpty()) return;

        Mob spawned = result.get();
        InfluencedEntityTracker tracker = WorldRegistry.get(level)
                .getTrackerManager()
                .getInfluencedEntityTracker();
        tracker.record(spawned, serverPlayer.getUUID());
    }
}