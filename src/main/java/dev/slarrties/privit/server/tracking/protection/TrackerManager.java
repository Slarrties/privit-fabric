package dev.slarrties.privit.server.tracking.protection;

import dev.slarrties.privit.PrivitMod;
import dev.slarrties.privit.server.tracking.nbt.TrackerNbtPersistence;
import dev.slarrties.privit.server.tracking.origin.OriginTracker;
import dev.slarrties.privit.server.tracking.origin.EntityOriginTracker;
import dev.slarrties.privit.server.tracking.origin.AbstractEntityOriginTracker;
import dev.slarrties.privit.server.tracking.redstone.RedstoneTraceService;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;

import java.util.List;
import java.util.UUID;
import java.util.ArrayList;

public final class TrackerManager {

    private static final int CLEANUP_INTERVAL_TICKS = 12000; // ~10 min

    private final ServerWorld world;
    private final TrackerNbtPersistence trackerStorage;
    private final List<OriginTracker> allTrackers = new ArrayList<>();
    private final List<AbstractEntityOriginTracker> entityTrackers;

    private int tickCounter = 0;

    private final MinecartOriginTracker minecartOriginTracker;
    private final ExplosionOriginTracker explosionOriginTracker;
    private final BoatOriginTracker boatOriginTracker;
    private final LightningOriginTracker lightningOriginTracker;
    private final MinecartFuelTracker minecartFuelTracker;
    private final FireOriginTracker fireOriginTracker;
    private final RedstoneOriginTracker redstoneOriginTracker;
    private final WitherPlacerTracker witherPlacerTracker;
    private final NetherPortalEntryTracker netherPortalEntryTracker;
    private final RedstoneTraceService redstoneTraceService;
    private final InfluencedEntityTracker influencedEntityTracker;
    private final FluidOriginTracker fluidOriginTracker;
    private final IceOriginTracker iceOriginTracker;
    private final HeatSourceOriginTracker heatSourceOriginTracker;
    private final CampfireOriginTracker campfireOriginTracker;

    public TrackerManager(ServerWorld world) {
        this.world = world;
        this.trackerStorage = new TrackerNbtPersistence(world);

        this.minecartOriginTracker = new MinecartOriginTracker(world);
        this.explosionOriginTracker = new ExplosionOriginTracker(world);
        this.boatOriginTracker = new BoatOriginTracker(world);
        this.lightningOriginTracker = new LightningOriginTracker(world);
        this.minecartFuelTracker = new MinecartFuelTracker(world);
        this.fireOriginTracker = new FireOriginTracker(world);
        this.redstoneOriginTracker = new RedstoneOriginTracker(world);
        this.witherPlacerTracker = new WitherPlacerTracker();
        this.netherPortalEntryTracker = new NetherPortalEntryTracker();
        this.influencedEntityTracker = new InfluencedEntityTracker(world);
        this.fluidOriginTracker = new FluidOriginTracker(world);
        this.iceOriginTracker = new IceOriginTracker(world);
        this.heatSourceOriginTracker = new HeatSourceOriginTracker(world);
        this.campfireOriginTracker = new CampfireOriginTracker(world);

        registerAll();

        this.entityTrackers = allTrackers.stream()
                .filter(AbstractEntityOriginTracker.class::isInstance)
                .map(AbstractEntityOriginTracker.class::cast)
                .toList();

        this.redstoneTraceService = new RedstoneTraceService(redstoneOriginTracker, world);

        trackerStorage.load(this);
    }

    private void registerAll() {
        allTrackers.add(minecartOriginTracker);
        allTrackers.add(explosionOriginTracker);
        allTrackers.add(boatOriginTracker);
        allTrackers.add(lightningOriginTracker);
        allTrackers.add(minecartFuelTracker);
        allTrackers.add(fireOriginTracker);
        allTrackers.add(redstoneOriginTracker);
        allTrackers.add(witherPlacerTracker);
        allTrackers.add(netherPortalEntryTracker);
        allTrackers.add(influencedEntityTracker);
        allTrackers.add(fluidOriginTracker);
        allTrackers.add(iceOriginTracker);
        allTrackers.add(heatSourceOriginTracker);
        allTrackers.add(campfireOriginTracker);
    }

    public void onServerTick() {
        if (++tickCounter % CLEANUP_INTERVAL_TICKS != 0) {
            return;
        }
        tickCounter = 0;

//        PrivitMod.LOGGER.info("[TrackerManager] Running periodic cleanup for dimension {}", world.getRegistryKey().getValue());

        for (OriginTracker tracker : allTrackers) {
            tracker.onServerTick(world);
        }
    }

    public void onWorldUnload() {
        trackerStorage.save(this);
        for (OriginTracker tracker : allTrackers) {
            tracker.onWorldUnload();
        }
    }

    public void clearAll() {
        for (OriginTracker tracker : allTrackers) {
            tracker.clearAll();
        }
    }

    public void onEntityRemoved(Entity entity) {
        for (OriginTracker tracker : allTrackers) {
            if (tracker instanceof EntityOriginTracker et) {
                et.remove(entity);
            }
        }
    }

    public void transferEntity(Entity original, Entity newEntity, TrackerManager destination) {
        for (int i = 0; i < entityTrackers.size(); i++) {
            AbstractEntityOriginTracker from = entityTrackers.get(i);
            AbstractEntityOriginTracker to = destination.entityTrackers.get(i);

            UUID playerId = from.getResponsible(original);
            if (playerId == null) continue;

            from.remove(original);
            to.record(newEntity, playerId);
        }
    }

    public MinecartOriginTracker getMinecartOriginTracker() { return minecartOriginTracker; }
    public ExplosionOriginTracker getExplosionOriginTracker() { return explosionOriginTracker; }
    public BoatOriginTracker getBoatOriginTracker() { return boatOriginTracker; }
    public LightningOriginTracker getLightningOriginTracker() { return lightningOriginTracker; }
    public MinecartFuelTracker getMinecartFuelTracker() { return minecartFuelTracker; }
    public FireOriginTracker getFireOriginTracker() { return fireOriginTracker; }
    public RedstoneOriginTracker getRedstoneOriginTracker() { return redstoneOriginTracker; }
    public WitherPlacerTracker getWitherPlacerTracker() { return witherPlacerTracker; }
    public NetherPortalEntryTracker getNetherPortalEntryTracker() { return netherPortalEntryTracker; }
    public RedstoneTraceService getRedstoneTraceService() {
        return redstoneTraceService;
    }
    public InfluencedEntityTracker getInfluencedEntityTracker() { return influencedEntityTracker; }
    public FluidOriginTracker getFluidOriginTracker() { return fluidOriginTracker; }
    public IceOriginTracker getIceOriginTracker() { return iceOriginTracker; }
    public HeatSourceOriginTracker getHeatSourceOriginTracker() { return heatSourceOriginTracker; }
    public CampfireOriginTracker getCampfireOriginTracker() { return campfireOriginTracker; }

    public NbtCompound saveToNbt() {
        NbtCompound root = new NbtCompound();

        for (OriginTracker tracker : allTrackers) {
            if (tracker.isPersistent()) {
                NbtCompound data = tracker.toNbt();
                if (!data.isEmpty()) {
                    root.put(tracker.getClass().getSimpleName(), data);
                }
            }
        }
        return root;
    }

    public void loadFromNbt(NbtCompound root) {
        if (root == null) return;

        for (OriginTracker tracker : allTrackers) {
            if (tracker.isPersistent()) {
                String key = tracker.getClass().getSimpleName();
                if (root.contains(key)) {
                    tracker.fromNbt(root.getCompound(key));
                }
            }
        }
    }
}