package dev.slarrties.privit.server.region.protection.mixin.use_fire_starters;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;

import net.minecraft.util.math.BlockPos;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.server.network.ServerPlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@AssociatedRule(Rule.USE_FIRE_STARTERS)
@Mixin(Entity.class)
public abstract class FireStarterBoatProtectionMixin {

    @Shadow
    public abstract BlockPos getBlockPos();

    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void preventFireStarterDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!(source.getAttacker() instanceof ServerPlayerEntity player)) return;
        if (source.isOf(DamageTypes.ON_FIRE) || source.isOf(DamageTypes.IN_FIRE)) {
            boolean allowed = RegionPermissionChecker.isAllowed(player, Rule.USE_FIRE_STARTERS, this.getBlockPos());

            if (!allowed) cir.setReturnValue(false);
        }
    }
}
