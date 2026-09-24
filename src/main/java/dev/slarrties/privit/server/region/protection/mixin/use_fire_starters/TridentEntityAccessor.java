package dev.slarrties.privit.server.region.protection.mixin.use_fire_starters;

import net.minecraft.item.ItemStack;
import net.minecraft.entity.projectile.TridentEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TridentEntity.class)
public interface TridentEntityAccessor {

    @Accessor("tridentStack")
    ItemStack privit$getTridentStack();

}