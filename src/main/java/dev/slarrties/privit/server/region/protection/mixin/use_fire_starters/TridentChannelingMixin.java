package dev.slarrties.privit.server.region.protection.mixin.use_fire_starters;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.protection.AssociatedRule;

import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.enchantment.EnchantmentHelper;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@AssociatedRule(Rule.USE_FIRE_STARTERS)
@Mixin(TridentEntity.class)
public abstract class TridentChannelingMixin {

    @Shadow private ItemStack tridentStack;

    @Inject(method = "onEntityHit", at = @At("HEAD"))
    private void onTridentHitEntity(EntityHitResult entityHitResult, CallbackInfo ci) {
        TridentEntity trident = (TridentEntity) (Object) this;

        if (!(trident.getOwner() instanceof ServerPlayerEntity player)) return;
        if (!(trident.getWorld() instanceof ServerWorld serverWorld)) return;

        int channelingLevel = EnchantmentHelper.getLevel(Enchantments.CHANNELING, tridentStack);
        if (channelingLevel > 0) {
            WorldRegistry.get(serverWorld)
                    .getTrackerManager()
                    .getLightningOriginTracker()
                    .record(trident, player.getUuid());
        }
    }
}