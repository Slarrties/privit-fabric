package dev.slarrties.privit.server.region.protection.mixin.spread_sculk;

import dev.slarrties.privit.server.tracking.context.SculkCursorDuck;
import dev.slarrties.privit.server.tracking.context.SculkBloomContext;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.SculkSpreader;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;


@Mixin(SculkSpreader.ChargeCursor.class)
public abstract class SculkCursorMixin implements SculkCursorDuck {

    @Shadow
    private BlockPos pos;

    @Shadow
    private int charge;

    @Unique
    private UUID responsible;

    @Override
    public @Nullable UUID getResponsible() {
        return this.responsible;
    }

    @Override
    public void setResponsible(@Nullable UUID responsible) {
        this.responsible = responsible;
    }

    @Inject(method = "mergeWith", at = @At("HEAD"))
    private void onMerge(SculkSpreader.ChargeCursor other, CallbackInfo ci) {
        SculkCursorDuck self = (SculkCursorDuck) (Object) this;
        SculkCursorDuck another = (SculkCursorDuck) (Object) other;
        if (self.getResponsible() == null && another.getResponsible() != null) {
            self.setResponsible(another.getResponsible());
        }
    }

    @Inject(method = "update", at = @At("HEAD"))
    private void beforeCursorUpdate(LevelAccessor level, BlockPos originPos, RandomSource random,
                                    SculkSpreader spreader, boolean spreadVeins, CallbackInfo ci) {
        if (this.responsible != null) SculkBloomContext.push(this.responsible);
    }

    @Inject(method = "update", at = @At("RETURN"))
    private void afterCursorUpdate(LevelAccessor level, BlockPos originPos, RandomSource random,
                                   SculkSpreader spreader, boolean spreadVeins, CallbackInfo ci) {
        if (SculkBloomContext.getCurrent() != null) SculkBloomContext.pop();
    }
}