package dev.slarrties.privit.server.region.protection.mixin.use_leashes;

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
import net.minecraft.world.entity.decoration.BlockAttachedEntity;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@AssociatedRule(Rule.USE_LEASHES)
@Mixin(BlockAttachedEntity.class)
public abstract class DamageLeashMixin {

    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void preventKnotDamage(ServerLevel level, DamageSource source, float damage, CallbackInfoReturnable<Boolean> cir) {
        BlockAttachedEntity self = (BlockAttachedEntity) (Object) this;
        if (!(self instanceof LeashFenceKnotEntity)) return;
        if (self.level().isClientSide()) return;
        if (!(self.level() instanceof ServerLevel serverLevel)) return;

        UUID responsible = DamageResponsibilityChecker.getResponsibleAttacker(source, serverLevel);
        if (responsible == null) return;

        BlockPos pos = self.blockPosition();
        if (!RegionPermissionChecker.isAllowed(responsible, Rule.USE_LEASHES, pos, serverLevel)) {
            if (source.getEntity() instanceof ServerPlayer serverPlayer)
                PlayerNotification.trySend(serverPlayer, NotificationType.DENY_USE_LEASH, Color.RED);
            cir.setReturnValue(false);
        }
    }
}