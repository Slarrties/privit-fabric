package dev.slarrties.privit.server.region.protection.mixin.attack_passive_mobs;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.passive.*;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@AssociatedRule(Rule.ATTACK_PASSIVE_MOBS)
@Mixin(Entity.class)
public abstract class LightningStruckPassiveMobsMixin {

    @Inject(method = "onStruckByLightning", at = @At("HEAD"), cancellable = true)
    private void preventLightningStrikeOnPassiveMobs(ServerWorld world, LightningEntity lightning, CallbackInfo ci) {
        Entity entity = (Entity) (Object) this;

        if (entity.getWorld().isClient || !(entity.getWorld() instanceof ServerWorld serverWorld)) return;
        if (!isProtectedPassiveEntity(entity)) return;

        UUID responsible = WorldRegistry.get(world)
                .getTrackerManager()
                .getLightningOriginTracker()
                .getResponsible(lightning);

        if (responsible == null) {
            ServerPlayerEntity channeler = lightning.getChanneler();
            if (channeler != null) {
                responsible = channeler.getUuid();
            }
        }

        if (responsible == null) return;

        boolean allowed = RegionPermissionChecker.isAllowed(responsible, Rule.ATTACK_PASSIVE_MOBS, entity.getBlockPos(), serverWorld);
        if (!allowed) {
            ServerPlayerEntity serverPlayer = serverWorld.getServer()
                    .getPlayerManager()
                    .getPlayer(responsible);
            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_ATTACK_PASSIVE_MOB, Color.RED);
            ci.cancel();
        }
    }

    @Unique
    private static boolean isProtectedPassiveEntity(Entity entity) {
        return entity instanceof AnimalEntity ||
                entity instanceof VillagerEntity ||
                entity instanceof AllayEntity ||
                entity instanceof AxolotlEntity ||
                entity instanceof WanderingTraderEntity;
    }
}