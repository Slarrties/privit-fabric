package dev.slarrties.privit.server.region.protection.mixin.use_fire_starters;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.damagesource.DamageSource;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@AssociatedRule(Rule.USE_FIRE_STARTERS)
@Mixin(VehicleEntity.class)
public abstract class FireStarterBoatProtectionMixin {

    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void preventFireStarterDamage(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        VehicleEntity self = (VehicleEntity) (Object) this;

        if (!(self instanceof AbstractBoat)) return;
        if (!(source.getEntity() instanceof ServerPlayer player)) return;
        if (source.is(DamageTypes.ON_FIRE) || source.is(DamageTypes.IN_FIRE)) {
            boolean allowed = RegionPermissionChecker.isAllowed(player.getUUID(), Rule.USE_FIRE_STARTERS, self.blockPosition(), level);
            if (!allowed) cir.setReturnValue(false);
        }
    }
}