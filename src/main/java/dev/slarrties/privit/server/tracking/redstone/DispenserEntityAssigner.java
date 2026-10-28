package dev.slarrties.privit.server.tracking.redstone;

import dev.slarrties.privit.PrivitMod;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.tracking.protection.BoatOriginTracker;
import dev.slarrties.privit.server.tracking.protection.ExplosionOriginTracker;
import dev.slarrties.privit.server.tracking.protection.InfluencedEntityTracker;
import dev.slarrties.privit.server.tracking.protection.MinecartOriginTracker;
import dev.slarrties.privit.server.tracking.redstone.handler.RedstoneReceiverHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class DispenserEntityAssigner {

    private DispenserEntityAssigner() {}

    public static void assignOwner(ServerLevel level, BlockPos dispenserPos, @Nullable EntityType<?> entityType, ItemStack stack) {
        UUID responsible = RedstoneReceiverHandler.findResponsiblePlayer(level, dispenserPos);
        if (responsible == null) return;

        if (entityType == null)
            entityType = getEntityTypeFromStack(stack);
        if (entityType == EntityType.TNT) {
            assignToTnt(level, dispenserPos, responsible);
        } else if (isProjectile(entityType)) {
            assignToProjectile(level, dispenserPos, responsible);
        } else if (entityType == EntityType.TNT_MINECART || stack.getItem() instanceof MinecartItem) {
            assignToMinecart(level, dispenserPos, responsible);
        } else if (stack.getItem() instanceof BoatItem) {
            assignToBoat(level, dispenserPos, responsible);
        } else if (stack.getItem() instanceof SpawnEggItem) {
            assignToInfluencedEntity(level, dispenserPos, responsible);
        } else if (stack.getItem() instanceof BucketItem) {
            assignToInfluencedEntity(level, dispenserPos, responsible);
        }
    }

    private static void assignToInfluencedEntity(ServerLevel level, BlockPos pos, UUID responsible) {
        InfluencedEntityTracker influencedEntityTracker = WorldRegistry.get(level)
                .getTrackerManager()
                .getInfluencedEntityTracker();
        AABB box = new AABB(pos).inflate(1.0);
        level.getEntitiesOfClass(LivingEntity.class, box, e -> influencedEntityTracker.getResponsible(e) == null)
                .forEach(e -> influencedEntityTracker.record(e, responsible));
    }

    private static void assignToProjectile(ServerLevel level, BlockPos pos, UUID responsible) {
        AABB box = new AABB(pos).inflate(2.0);
        ServerPlayer serverPlayer = level.getServer()
                .getPlayerList()
                .getPlayer(responsible);

        if (serverPlayer != null) {
            level.getEntitiesOfClass(Projectile.class, box, p -> p.getOwner() == null)
                    .forEach(projectile -> projectile.setOwner(serverPlayer));
        } else {
            PrivitMod.LOGGER.warn("[DispenserEntityAssigner] Could not find player {} ", responsible);
        }
    }

    private static void assignToTnt(ServerLevel level, BlockPos pos, UUID responsible) {
        ExplosionOriginTracker explosionOriginTracker = WorldRegistry.get(level)
                .getTrackerManager()
                .getExplosionOriginTracker();
        AABB box = new AABB(pos).inflate(2.0);
        level.getEntitiesOfClass(PrimedTnt.class, box,
                        tnt -> explosionOriginTracker.getResponsible(tnt) == null)
                .forEach(tnt -> explosionOriginTracker.record(tnt, responsible));
    }

    private static void assignToMinecart(ServerLevel level, BlockPos pos, UUID responsible) {
        MinecartOriginTracker minecartOriginTracker = WorldRegistry.get(level)
                .getTrackerManager()
                .getMinecartOriginTracker();
        AABB box = new AABB(pos).inflate(2.0);
        level.getEntitiesOfClass(AbstractMinecart.class, box,
                        cart -> minecartOriginTracker.getResponsible(cart) == null)
                .forEach(cart -> minecartOriginTracker.record(cart, responsible));
    }

    private static void assignToBoat(ServerLevel level, BlockPos pos, UUID responsible) {
        BoatOriginTracker boatOriginTracker = WorldRegistry.get(level).getTrackerManager().getBoatOriginTracker();
        AABB box = new AABB(pos).inflate(2.0);
        level.getEntitiesOfClass(Boat.class, box,
                        boat -> boatOriginTracker.getResponsible(boat) == null)
                .forEach(boat -> boatOriginTracker.record(boat, responsible));
    }

    @Nullable
    private static EntityType<?> getEntityTypeFromStack(ItemStack stack) {
        if (stack.is(Items.TNT)) return EntityType.TNT;
        if (stack.is(Items.FIRE_CHARGE)) return EntityType.SMALL_FIREBALL;
        if (stack.is(Items.SNOWBALL)) return EntityType.SNOWBALL;
        if (stack.is(Items.EGG)) return EntityType.EGG;
        if (stack.is(Items.SPLASH_POTION)) return EntityType.SPLASH_POTION;
        if (stack.is(Items.LINGERING_POTION)) return EntityType.LINGERING_POTION;
        if (stack.is(Items.WIND_CHARGE)) return EntityType.WIND_CHARGE;
        if (stack.getItem() instanceof ArrowItem) return EntityType.ARROW;
        return null;
    }

    private static boolean isProjectile(@Nullable EntityType<?> type) {
        if (type == null) return false;
        return type == EntityType.SNOWBALL ||
                type == EntityType.EGG ||
                type == EntityType.LINGERING_POTION ||
                type == EntityType.SPLASH_POTION ||
                type == EntityType.WIND_CHARGE ||
                type == EntityType.ARROW ||
                type == EntityType.SPECTRAL_ARROW ||
                type == EntityType.SMALL_FIREBALL ||
                type == EntityType.FIREBALL;
    }
}