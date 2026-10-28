package dev.slarrties.privit.server.region.protection.mixin.interact_with_minecarts;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.protection.AssociatedRule;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.vehicle.minecart.MinecartTNT;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@AssociatedRule(Rule.INTERACT_WITH_MINECARTS)
@Mixin(AbstractMinecart.class)
public abstract class MinecartCreationMixin {

    @Inject(method = "createMinecart", at = @At("RETURN"))
    private static void onMinecartCreated(
            Level level, double x, double y, double z,
            EntityType<?> type, EntitySpawnReason reason, ItemStack stack,
            Player player, CallbackInfoReturnable<AbstractMinecart> cir
    ) {
        AbstractMinecart minecart = cir.getReturnValue();
        if (minecart == null || player == null) return;
        if (minecart instanceof MinecartTNT && level instanceof ServerLevel serverLevel) {
            WorldRegistry.get(serverLevel)
                    .getTrackerManager()
                    .getExplosionOriginTracker()
                    .record(minecart, player.getUUID());
        }
    }
}