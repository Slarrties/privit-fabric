package dev.slarrties.privit.server.region.protection.mixin.attack_passive_mobs;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@AssociatedRule(Rule.ATTACK_PASSIVE_MOBS)
@Mixin(Entity.class)
public abstract class LightningStruckPassiveMobsMixin {

    @Inject(method = "thunderHit", at = @At("HEAD"), cancellable = true)
    private void preventLightningStrikeOnPassiveMobs(ServerLevel level, LightningBolt lightning, CallbackInfo ci) {
        Entity entity = (Entity) (Object) this;

        if (entity.level().isClientSide() || !(entity.level() instanceof ServerLevel serverLevel)) return;
        if (!isProtectedPassiveEntity(entity)) return;

        UUID responsible = WorldRegistry.get(serverLevel)
                .getTrackerManager()
                .getLightningOriginTracker()
                .getResponsible(lightning);

        if (responsible == null) {
            ServerPlayer cause = lightning.getCause();
            if (cause != null) responsible = cause.getUUID();
        }

        if (responsible == null) return;

        boolean allowed = RegionPermissionChecker.isAllowed(responsible, Rule.ATTACK_PASSIVE_MOBS, entity.blockPosition(), serverLevel);
        if (!allowed) {
            ServerPlayer serverPlayer = serverLevel.getServer()
                    .getPlayerList()
                    .getPlayer(responsible);
            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_ATTACK_PASSIVE_MOB, Color.RED);
            ci.cancel();
        }
    }

    @Unique
    private static boolean isProtectedPassiveEntity(Entity entity) {
        return entity instanceof Animal ||
                entity instanceof Villager ||
                entity instanceof Allay ||
                entity instanceof Axolotl ||
                entity instanceof WanderingTrader;
    }
}