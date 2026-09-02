package dev.slarrties.privit.server.tracking.redstone;

import dev.slarrties.privit.PrivitMod;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.tracking.protection.BoatOriginTracker;
import dev.slarrties.privit.server.tracking.protection.ExplosionOriginTracker;
import dev.slarrties.privit.server.tracking.protection.InfluencedEntityTracker;
import dev.slarrties.privit.server.tracking.protection.MinecartOriginTracker;
import dev.slarrties.privit.server.tracking.redstone.handler.RedstoneReceiverHandler;

import net.minecraft.item.*;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.BlockPos;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class DispenserEntityAssigner {

    private DispenserEntityAssigner() {}

    public static void assignOwner(ServerWorld serverWorld, BlockPos dispenserPos, @Nullable EntityType<?> entityType, ItemStack stack) {
        UUID responsible = RedstoneReceiverHandler.findResponsiblePlayer(serverWorld, dispenserPos);

        if (responsible == null) return;
        if (entityType == null)
            entityType = getEntityTypeFromStack(stack);
        if (entityType == EntityType.TNT) {
            assignToTnt(serverWorld, dispenserPos, responsible);
        } else if (isProjectile(entityType)) {
            assignToProjectile(serverWorld, dispenserPos, responsible);
        } else if (entityType == EntityType.TNT_MINECART || stack.getItem() instanceof MinecartItem) {
            assignToMinecart(serverWorld, dispenserPos, responsible);
        } else if (stack.getItem() instanceof BoatItem) {
            assignToBoat(serverWorld, dispenserPos, responsible);
        } else if (stack.getItem() instanceof SpawnEggItem) {
            assignToInfluencedEntity(serverWorld, dispenserPos, responsible);
        } else if (stack.getItem() instanceof BucketItem) {
            assignToInfluencedEntity(serverWorld, dispenserPos, responsible);
        }
    }

    // TODO: can record a player without the culprit if they are within range
    private static void assignToInfluencedEntity(ServerWorld world, BlockPos pos, UUID responsible) {
        InfluencedEntityTracker influencedEntityTracker = WorldRegistry.get(world)
                .getTrackerManager()
                .getInfluencedEntityTracker();
        Box box = new Box(pos).expand(1.0);

        world.getEntitiesByClass(LivingEntity.class, box, e -> influencedEntityTracker.getResponsible(e) == null)
                .forEach(e -> influencedEntityTracker.record(e, responsible));
    }

    private static void assignToProjectile(ServerWorld serverWorld, BlockPos pos, UUID responsible) {
        Box box = new Box(pos).expand(2.0);
        ServerPlayerEntity serverPlayer = serverWorld.getServer()
                .getPlayerManager()
                .getPlayer(responsible);

        if(serverPlayer != null) {
            serverWorld.getEntitiesByClass(ProjectileEntity.class, box, p -> p.getOwner() == null)
                    .forEach(projectile -> projectile.setOwner(serverPlayer));
        } else {
            PrivitMod.LOGGER.warn("[DispenserEntityAssigner] Could not find player {} ", responsible);
        }
    }

    private static void assignToTnt(ServerWorld world, BlockPos pos, UUID responsible) {
        ExplosionOriginTracker explosionOriginTracker = WorldRegistry.get(world)
                .getTrackerManager()
                .getExplosionOriginTracker();
        Box box = new Box(pos).expand(2.0);

        world.getEntitiesByClass(TntEntity.class, box,
                        tnt -> explosionOriginTracker.getResponsiblePlayer(tnt) == null)
                .forEach(tnt -> explosionOriginTracker.record(tnt, responsible));
    }

    private static void assignToMinecart(ServerWorld world, BlockPos pos, UUID responsible) {
        MinecartOriginTracker minecartOriginTracker = WorldRegistry.get(world)
                .getTrackerManager()
                .getMinecartOriginTracker();
        Box box = new Box(pos).expand(2.0);

        world.getEntitiesByClass(AbstractMinecartEntity.class, box,
                        cart -> minecartOriginTracker.getResponsiblePlayer(cart) == null)
                .forEach(cart -> minecartOriginTracker.record(cart, responsible));
    }

    private static void assignToBoat(ServerWorld world, BlockPos pos, UUID responsible) {
        BoatOriginTracker boatOriginTracker = WorldRegistry.get(world).getTrackerManager().getBoatOriginTracker();
        Box box = new Box(pos).expand(2.0);

        world.getEntitiesByClass(BoatEntity.class, box,
                        boat -> boatOriginTracker.getResponsiblePlayer(boat) == null)
                .forEach(boat -> boatOriginTracker.record(boat, responsible));
    }

    @Nullable
    private static EntityType<?> getEntityTypeFromStack(ItemStack stack) {
        if (stack.isOf(Items.TNT)) return EntityType.TNT;
        if (stack.isOf(Items.FIRE_CHARGE)) return EntityType.SMALL_FIREBALL;
        if (stack.isOf(Items.SNOWBALL)) return EntityType.SNOWBALL;
        if (stack.isOf(Items.EGG)) return EntityType.EGG;
        if (stack.isOf(Items.SPLASH_POTION) || stack.isOf(Items.LINGERING_POTION)) return EntityType.POTION;
        if (stack.isOf(Items.WIND_CHARGE)) return EntityType.WIND_CHARGE;
        if (stack.getItem() instanceof ArrowItem) return EntityType.ARROW;

        return null;
    }

    private static boolean isProjectile(@Nullable EntityType<?> type) {
        if (type == null) return false;
        return type == EntityType.SNOWBALL ||
                type == EntityType.EGG ||
                type == EntityType.POTION ||
                type == EntityType.WIND_CHARGE ||
                type == EntityType.ARROW ||
                type == EntityType.SPECTRAL_ARROW ||
                type == EntityType.SMALL_FIREBALL ||
                type == EntityType.FIREBALL;
    }
}