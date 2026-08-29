package dev.slarrties.privit.server.region.protection.mixin.spread_sculk;

import dev.slarrties.privit.server.tracking.context.SculkCursorDuck;
import dev.slarrties.privit.server.tracking.context.SculkBloomContext;

import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.block.entity.SculkSpreadManager;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.UUID;

@Mixin(SculkSpreadManager.class)
public abstract class SculkSpreadManagerMixin {

    @Unique
    private static final String RESPONSIBLE_KEY = "PrivitSculkResponsible";

    @Shadow
    private List<SculkSpreadManager.Cursor> cursors;

    @Inject(method = "addCursor", at = @At("HEAD"))
    private void privit$onAddCursor(SculkSpreadManager.Cursor cursor, CallbackInfo ci) {
        UUID responsible = SculkBloomContext.getResponsible();

        if (responsible == null) return;
        if (cursor instanceof SculkCursorDuck duck) {
            duck.setResponsible(responsible);
        }
    }

    @Inject(method = "writeNbt", at = @At("RETURN"))
    private void privit$writeResponsible(NbtCompound nbt, CallbackInfo ci) {
        NbtList list = new NbtList();

        for (SculkSpreadManager.Cursor cursor : this.cursors) {
            NbtCompound entry = new NbtCompound();

            if (cursor instanceof SculkCursorDuck duck) {
                UUID responsible = duck.getResponsible();

                if (responsible != null)
                    entry.putUuid("uuid", responsible);
            }
            list.add(entry);
        }

        nbt.put(RESPONSIBLE_KEY, list);
    }

    @Inject(method = "readNbt", at = @At("RETURN"))
    private void privit$readResponsible(NbtCompound nbt, CallbackInfo ci) {
        if (!nbt.contains(RESPONSIBLE_KEY, NbtElement.LIST_TYPE)) return;

        NbtList list = nbt.getList(RESPONSIBLE_KEY, NbtElement.COMPOUND_TYPE);
        int count = Math.min(list.size(), this.cursors.size());

        for (int i = 0; i < count; i++) {
            NbtCompound entry = list.getCompound(i);
            if (!entry.containsUuid("uuid")) continue;

            SculkSpreadManager.Cursor cursor = this.cursors.get(i);

            if (cursor instanceof SculkCursorDuck duck) {
                UUID responsible = entry.getUuid("uuid");
                duck.setResponsible(responsible);
            }
        }
    }
}