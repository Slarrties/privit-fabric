package dev.slarrties.privit.server.region.protection.mixin.use_fire_starters;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.world.WorldRegistry;

import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@AssociatedRule(Rule.USE_FIRE_STARTERS)
@Mixin(TridentEntity.class)
public abstract class TridentChannelingMixin {

    @Inject(
            method = "onBlockHitEnchantmentEffects",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/enchantment/EnchantmentHelper;onHitBlock(" +
                            "Lnet/minecraft/server/world/ServerWorld;" +
                            "Lnet/minecraft/item/ItemStack;" +
                            "Lnet/minecraft/entity/LivingEntity;" +
                            "Lnet/minecraft/entity/Entity;" +
                            "Lnet/minecraft/entity/EquipmentSlot;" +
                            "Lnet/minecraft/util/math/Vec3d;" +
                            "Lnet/minecraft/block/BlockState;" +
                            "Ljava/util/function/Consumer;)V",
                    shift = At.Shift.AFTER
            )
    )
    private void trackChannelingLightning(ServerWorld world, BlockHitResult blockHitResult, ItemStack weaponStack, CallbackInfo ci) {
        TridentEntity trident = (TridentEntity) (Object) this;
        if (!(trident.getOwner() instanceof ServerPlayerEntity player) ||
                !(trident.getOwner().getWorld() instanceof ServerWorld serverWorld)) return;

        RegistryEntry<Enchantment> channelingEntry =
                world.getRegistryManager()
                        .get(RegistryKeys.ENCHANTMENT)
                        .getEntry(Enchantments.CHANNELING)
                        .orElse(null);

        if (channelingEntry == null) return;

        int level = EnchantmentHelper.getLevel(channelingEntry, weaponStack);

        if (level > 0) {
            WorldRegistry.get(serverWorld)
                    .getTrackerManager()
                    .getLightningOriginTracker()
                    .record(trident, player.getUuid());
        }
    }
}