package dev.slarrties.privit.server.region.protection.mixin.build.entity;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.decoration.painting.Painting;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@AssociatedRule(Rule.BUILD)
@Mixin(Player.class)
public abstract class BuildEntityAttackMixin {

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void preventAttackOnProtectedEntities(Entity target, CallbackInfo ci) {
        Player player = (Player) (Object) this;

        if (!(player instanceof ServerPlayer serverPlayer)) return;
        if (serverPlayer.level().isClientSide()) return;
        if (!isProtectedDecoration(target)) return;

        BlockPos pos = target.blockPosition();
        if (!RegionPermissionChecker.isAllowed(serverPlayer.getUUID(), Rule.BUILD, pos, serverPlayer.level())) {
            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_BREAK_BLOCK, Color.RED);
            ci.cancel();
        }
    }

    @Unique
    private static boolean isProtectedDecoration(Entity entity) {
        return entity instanceof ItemFrame
                || entity instanceof Painting
                || entity instanceof ArmorStand
                || entity instanceof EndCrystal;
    }
}