package dev.slarrties.privit.server.region.protection.mixin.use_fire_starters;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.protection.AssociatedRule;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@AssociatedRule(Rule.USE_FIRE_STARTERS)
@Mixin(ThrownTrident.class)
public abstract class TridentChannelingMixin {

    @Inject(method = "hitBlockEnchantmentEffects", at = @At("TAIL"))
    private void trackChannelingLightning(ServerLevel level, BlockHitResult hitResult, ItemStack weapon, CallbackInfo ci) {
        ThrownTrident trident = (ThrownTrident) (Object) this;
        if (!(trident.getOwner() instanceof ServerPlayer player)) return;

        Holder<Enchantment> channelingHolder = level.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.CHANNELING);

        if (weapon.getEnchantments().getLevel(channelingHolder) > 0) {
            WorldRegistry.get(level)
                    .getTrackerManager()
                    .getLightningOriginTracker()
                    .record(trident, player.getUUID());
        }
    }
}