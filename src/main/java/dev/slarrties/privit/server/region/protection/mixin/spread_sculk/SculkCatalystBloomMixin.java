package dev.slarrties.privit.server.region.protection.mixin.spread_sculk;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.tracking.context.SculkBloomContext;

import net.minecraft.core.Holder;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.block.entity.SculkCatalystBlockEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@AssociatedRule(Rule.SPREAD_SCULK)
@Mixin(SculkCatalystBlockEntity.CatalystListener.class)
public abstract class SculkCatalystBloomMixin {

    @Inject(method = "handleGameEvent", at = @At("HEAD"))
    private void beforeHandle(ServerLevel level, Holder<GameEvent> event, GameEvent.Context context, Vec3 sourcePosition, CallbackInfoReturnable<Boolean> cir) {
        Entity entity = context.sourceEntity();
        if (!(entity instanceof LivingEntity livingEntity)) return;

        UUID responsible = resolveResponsible(livingEntity, level);
        if (responsible != null) {
            SculkBloomContext.push(responsible);
        }
    }

    @Inject(method = "handleGameEvent", at = @At("RETURN"))
    private void afterHandle(ServerLevel level, Holder<GameEvent> event, GameEvent.Context context, Vec3 sourcePosition, CallbackInfoReturnable<Boolean> cir) {
        if (SculkBloomContext.getCurrent() != null) {
            SculkBloomContext.pop();
        }
    }

    @Unique
    private UUID resolveResponsible(LivingEntity entity, ServerLevel level) {
        if (entity instanceof ServerPlayer player) return player.getUUID();
        return WorldRegistry.get(level)
                .getTrackerManager()
                .getInfluencedEntityTracker()
                .getResponsible(entity);
    }
}