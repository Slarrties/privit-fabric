package dev.slarrties.privit.server.region.protection.mixin.interact_with_minecarts;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.protection.AssociatedRule;

import net.minecraft.util.ActionResult;
import net.minecraft.item.MinecartItem;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.TntMinecartEntity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;

import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@AssociatedRule(Rule.INTERACT_WITH_MINECARTS)
@Mixin(MinecartItem.class)
public abstract class MinecartCreationMixin {

    @Inject(
            method = "useOnBlock",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;spawnEntity(Lnet/minecraft/entity/Entity;)Z")
    )
    private void onMinecartSpawned(
            ItemUsageContext context,
            CallbackInfoReturnable<ActionResult> cir,
            @Local AbstractMinecartEntity entity
    ) {
        if (!(context.getWorld() instanceof ServerWorld serverWorld)) return;

        PlayerEntity player = context.getPlayer();
        if (player == null) return;
        if (entity instanceof TntMinecartEntity minecart) {
            WorldRegistry.get(serverWorld)
                    .getTrackerManager()
                    .getExplosionOriginTracker()
                    .record(minecart, player.getUuid());
        }
    }
}