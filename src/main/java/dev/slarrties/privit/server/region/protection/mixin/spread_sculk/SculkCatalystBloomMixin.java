package dev.slarrties.privit.server.region.protection.mixin.spread_sculk;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.tracking.context.SculkBloomContext;

import net.minecraft.util.math.Vec3d;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.event.GameEvent;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.block.entity.SculkCatalystBlockEntity;

import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@AssociatedRule(Rule.SPREAD_SCULK)
@Mixin(SculkCatalystBlockEntity.Listener.class)
public abstract class SculkCatalystBloomMixin {

    @Inject(
            method = "listen",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/block/entity/SculkSpreadManager;spread(Lnet/minecraft/util/math/BlockPos;I)V"
            )
    )
    private void beforeSpread(ServerWorld world, RegistryEntry<GameEvent> event, GameEvent.Emitter emitter, Vec3d emitterPos,
                              CallbackInfoReturnable<Boolean> cir, @Local LivingEntity livingEntity) {
        UUID responsible = resolveResponsible(livingEntity, world);
        if (responsible == null) return;

        SculkBloomContext.push(responsible);
    }

    @Inject(
            method = "listen",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/block/entity/SculkSpreadManager;spread(Lnet/minecraft/util/math/BlockPos;I)V",
                    shift = At.Shift.AFTER
            )
    )
    private void afterSpread(ServerWorld world, RegistryEntry<GameEvent> event, GameEvent.Emitter emitter,
                             Vec3d emitterPos, CallbackInfoReturnable<Boolean> cir) {
        if (SculkBloomContext.getCurrent() != null) {
            SculkBloomContext.pop();
        }
    }

    @Unique
    private static UUID resolveResponsible(LivingEntity entity, ServerWorld world) {
        if (entity instanceof ServerPlayerEntity player) return player.getUuid();

        return WorldRegistry.get(world)
                .getTrackerManager()
                .getInfluencedEntityTracker()
                .getResponsible(entity);
    }
}