package dev.slarrties.privit.server.region.protection.mixin.spread_sculk;

import dev.slarrties.privit.server.tracking.context.SculkCursorDuck;
import dev.slarrties.privit.server.tracking.context.SculkBloomContext;

import net.minecraft.world.level.block.SculkSpreader;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.UUID;

@Mixin(SculkSpreader.class)
public abstract class SculkSpreadManagerMixin {

    @Shadow
    private List<SculkSpreader.ChargeCursor> cursors;

    @Inject(method = "addCursor", at = @At("HEAD"))
    private void privit$onAddCursor(SculkSpreader.ChargeCursor cursor, CallbackInfo ci) {
        UUID responsible = SculkBloomContext.getResponsible();
        if (responsible == null) return;
        if (cursor instanceof SculkCursorDuck duck) duck.setResponsible(responsible);
    }
}