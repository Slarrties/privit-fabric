package dev.slarrties.privit.server.region.protection.mixin.interact_with_minecarts;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.protection.AssociatedRule;

import net.minecraft.item.ItemStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.TntMinecartEntity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.server.world.ServerWorld;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@AssociatedRule(Rule.INTERACT_WITH_MINECARTS)
@Mixin(AbstractMinecartEntity.class)
public abstract class MinecartCreationMixin {

    @Inject(
            method = "create(Lnet/minecraft/server/world/ServerWorld;DDDLnet/minecraft/entity/vehicle/AbstractMinecartEntity$Type;Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/player/PlayerEntity;)Lnet/minecraft/entity/vehicle/AbstractMinecartEntity;",
            at = @At("RETURN")
    )
    private static void onMinecartCreated(
            ServerWorld world, double x, double y, double z,
            AbstractMinecartEntity.Type type, ItemStack stack,
            PlayerEntity player, CallbackInfoReturnable<AbstractMinecartEntity> cir) {

        AbstractMinecartEntity minecart = cir.getReturnValue();
        if (minecart == null || player == null) return;
        if (minecart instanceof TntMinecartEntity && minecart.getWorld() instanceof ServerWorld serverWorld) {
            WorldRegistry.get(serverWorld)
                    .getTrackerManager()
                    .getExplosionOriginTracker()
                    .record(minecart, player.getUuid());
        }
    }
}