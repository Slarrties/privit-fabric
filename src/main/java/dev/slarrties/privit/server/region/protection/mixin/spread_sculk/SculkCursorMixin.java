package dev.slarrties.privit.server.region.protection.mixin.spread_sculk;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.tracking.context.SculkCursorDuck;
import dev.slarrties.privit.server.tracking.context.SculkBloomContext;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.WorldAccess;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.block.entity.SculkSpreadManager;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import java.util.UUID;

@Mixin(SculkSpreadManager.Cursor.class)
public abstract class SculkCursorMixin implements SculkCursorDuck {

    @Shadow private BlockPos pos;

    @Shadow int charge;

    @Unique private UUID responsible;

    @Override
    public @Nullable UUID getResponsible() {
        return this.responsible;
    }

    @Override
    public void setResponsible(@Nullable UUID responsible) {
        this.responsible = responsible;
    }

    @Inject(method = "merge", at = @At("HEAD"))
    private void onMerge(SculkSpreadManager.Cursor other, CallbackInfo ci) {
        SculkCursorDuck self = (SculkCursorDuck) (Object) this;
        SculkCursorDuck another = (SculkCursorDuck) (Object) other;

        if (self.getResponsible() == null && another.getResponsible() != null) {
            self.setResponsible(another.getResponsible());
        }
    }

    @Inject(method = "spread", at = @At("HEAD"))
    private void beforeCursorSpread(WorldAccess world, BlockPos catalystPos, Random random, SculkSpreadManager spreadManager,
                                    boolean shouldConvertToBlock, CallbackInfo ci) {
        if (this.responsible != null) SculkBloomContext.push(this.responsible);
    }

    @Inject(method = "spread", at = @At("RETURN"))
    private void afterCursorSpread(WorldAccess world, BlockPos catalystPos, Random random, SculkSpreadManager spreadManager,
                                   boolean shouldConvertToBlock, CallbackInfo ci) {
        if (SculkBloomContext.getCurrent() != null) SculkBloomContext.pop();
    }

    @ModifyExpressionValue(
            method = "spread",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/block/entity/SculkSpreadManager$Cursor;getSpreadPos(Lnet/minecraft/world/WorldAccess;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/random/Random;)Lnet/minecraft/util/math/BlockPos;"
            )
    )
    private BlockPos filterSpreadPos(BlockPos original, WorldAccess world) {
        if (original == null) return null;
        if (!(world instanceof ServerWorld serverWorld)) return original;
        if (this.responsible == null) return original;

        boolean allowed = RegionPermissionChecker.isAllowed(this.responsible, Rule.SPREAD_SCULK, original, serverWorld);

        if (!allowed) {
            int oldCharge = this.charge;
            this.charge = Math.max(0, this.charge - Math.max(1, this.charge / 4));

            return null;
        }

        return original;
    }
}