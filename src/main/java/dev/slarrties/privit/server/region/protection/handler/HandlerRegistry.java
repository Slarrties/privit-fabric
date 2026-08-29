package dev.slarrties.privit.server.region.protection.handler;

import dev.slarrties.privit.server.region.protection.handler.attack_passive_mobs.AttackPassiveHandler;
import dev.slarrties.privit.server.region.protection.handler.eat_chorus_fruit.EatChorusFruitHandler;
import dev.slarrties.privit.server.region.protection.handler.interact_with_animals.InteractAnimalsHandler;
import dev.slarrties.privit.server.region.protection.handler.interact_with_boats.InteractionBoatHandler;
import dev.slarrties.privit.server.region.protection.handler.interact_with_containers.InteractContainersHandler;
import dev.slarrties.privit.server.region.protection.handler.interact_with_minecarts.InteractionMinecartHandler;
import dev.slarrties.privit.server.region.protection.handler.interact_with_signs.InteractSignHandler;
import dev.slarrties.privit.server.region.protection.handler.use_spawn_eggs.SpawnedEntityHandler;
import dev.slarrties.privit.server.region.protection.handler.use_trial_mechanics.InteractTrialSpawnerHandler;
import dev.slarrties.privit.server.region.protection.handler.use_trial_mechanics.InteractVaultHandler;
import dev.slarrties.privit.server.region.protection.handler.pvp.PvpHandler;
import dev.slarrties.privit.server.region.protection.handler.set_respawn_point.SetRespawnPointHandler;
import dev.slarrties.privit.server.region.protection.handler.use_fluids.UseFluidHandler;
import dev.slarrties.privit.server.region.protection.handler.throw_eggs.ThrowEggHandler;
import dev.slarrties.privit.server.region.protection.handler.throw_ender_pearls.ThrowEnderPearlHandler;
import dev.slarrties.privit.server.region.protection.handler.throw_potions.ThrowPotionHandler;
import dev.slarrties.privit.server.region.protection.handler.throw_snowballs.ThrowSnowballHandler;
import dev.slarrties.privit.server.region.protection.handler.throw_wind_charges.ThrowWindChargeHandler;
import dev.slarrties.privit.server.region.protection.handler.trade_with_villagers.TradeVillagerHandler;
import dev.slarrties.privit.server.region.protection.handler.use_fire_starters.UseFireStarterHandler;
import dev.slarrties.privit.server.region.protection.handler.use_fishing_rods.UseFishingRodHandler;
import dev.slarrties.privit.server.region.protection.handler.use_leashes.UseLeashHandler;
import dev.slarrties.privit.server.region.protection.handler.use_sponges.SpongeHandler;

public final class HandlerRegistry {

    private HandlerRegistry() {}

    public static void registerAll() {
        register(new TradeVillagerHandler());
        register(new AttackPassiveHandler());
        register(new InteractionBoatHandler());
        register(new InteractionMinecartHandler());
        register(new UseLeashHandler());
        register(new InteractAnimalsHandler());
        register(new ThrowPotionHandler());
        register(new UseFishingRodHandler());
        register(new ThrowEnderPearlHandler());
        register(new EatChorusFruitHandler());
        register(new UseFireStarterHandler());
        register(new ThrowWindChargeHandler());
        register(new ThrowSnowballHandler());
        register(new ThrowEggHandler());
        register(new UseFluidHandler());
        register(new InteractSignHandler());
        register(new InteractContainersHandler());
        register(new SetRespawnPointHandler());
        register(new PvpHandler());
        register(new SpongeHandler());
        register(new InteractTrialSpawnerHandler());
        register(new InteractVaultHandler());
        register(new SpawnedEntityHandler());
    }

    private static void register(RuleEventHandler handler) {
        handler.register();
    }
}