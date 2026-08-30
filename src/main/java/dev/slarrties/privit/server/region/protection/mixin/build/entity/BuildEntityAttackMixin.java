package dev.slarrties.privit.server.region.protection.mixin.build.entity;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;

import net.minecraft.util.math.BlockPos;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.server.network.ServerPlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@AssociatedRule(Rule.BUILD)
@Mixin(ServerPlayerEntity.class)
public abstract class BuildEntityAttackMixin {

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void preventAttackOnProtectedEntities(Entity target, CallbackInfo ci) {
        if (!isProtectedDecoration(target)) return;

        ServerPlayerEntity serverPlayer = (ServerPlayerEntity) (Object) this;
        BlockPos pos = target.getBlockPos();

        if (!RegionPermissionChecker.isAllowed(serverPlayer.getUuid(), Rule.BUILD, pos, serverPlayer.getServerWorld())) {
            ci.cancel();
            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_BREAK_BLOCK, Color.RED);
        }
    }

    @Unique
    private static boolean isProtectedDecoration(Entity entity) {
        return entity instanceof ItemFrameEntity
                || entity instanceof PaintingEntity
                || entity instanceof ArmorStandEntity
                || entity instanceof EndCrystalEntity;
    }
}