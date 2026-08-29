package dev.slarrties.privit.server.region.protection.mixin.use_leashes;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.util.DamageResponsibilityChecker;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;

import net.minecraft.util.math.BlockPos;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.BlockAttachedEntity;
import net.minecraft.entity.decoration.LeashKnotEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@AssociatedRule(Rule.USE_LEASHES)
@Mixin(BlockAttachedEntity.class)
public abstract class DamageLeashMixin {

    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void preventKnotDamage(DamageSource source, float amount,
                                   CallbackInfoReturnable<Boolean> cir) {
        BlockAttachedEntity self = (BlockAttachedEntity) (Object) this;
        if (!(self instanceof LeashKnotEntity)) return;
        if (self.getWorld().isClient()) return;
        if (!(self.getWorld() instanceof ServerWorld serverWorld)) return;

        UUID responsible = DamageResponsibilityChecker.getResponsibleAttacker(source, serverWorld);
        if (responsible == null) return;

        BlockPos pos = self.getBlockPos();
        if (!RegionPermissionChecker.isAllowed(responsible, Rule.USE_LEASHES, pos, serverWorld)) {
            if (source.getAttacker() instanceof ServerPlayerEntity serverPlayer) {
                PlayerNotification.trySend(serverPlayer, NotificationType.DENY_USE_LEASH, Color.RED);
            }

            cir.setReturnValue(false);
        }
    }
}