package dev.slarrties.privit.server.region.protection.mixin.build.entity;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.util.DamageResponsibilityChecker;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.decoration.painting.Painting;
import net.minecraft.world.entity.decoration.BlockAttachedEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@AssociatedRule(Rule.BUILD)
@Mixin(BlockAttachedEntity.class)
public abstract class PaintingDamageMixin {

    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void preventPaintingDamage(ServerLevel level, DamageSource source, float damage, CallbackInfoReturnable<Boolean> cir) {
        BlockAttachedEntity self = (BlockAttachedEntity) (Object) this;
        if (!(self instanceof Painting)) return;

        ServerLevel serverLevel = (ServerLevel) self.level();
        UUID responsible = DamageResponsibilityChecker.getResponsibleAttacker(source, serverLevel);
        if (responsible == null) return;

        BlockPos pos = self.blockPosition();
        if (!RegionPermissionChecker.isAllowed(responsible, Rule.BUILD, pos, serverLevel)) {
            if (source.getEntity() instanceof ServerPlayer serverPlayer) {
                PlayerNotification.trySend(serverPlayer, NotificationType.DENY_BREAK_BLOCK, Color.RED);
            }
            cir.setReturnValue(false);
        }
    }
}