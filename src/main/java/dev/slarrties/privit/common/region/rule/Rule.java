package dev.slarrties.privit.common.region.rule;

import net.minecraft.text.Text;

public enum Rule {
    MANAGE,
    PVP,
    BUILD,
    DROP_AND_PICKUP_ITEMS,
    ATTACK_PASSIVE_MOBS,
    CAUSE_EXPLOSIONS,
    INTERACT_WITH_ANIMALS,
    TRADE_WITH_VILLAGERS,
    SET_RESPAWN_POINT,
    INTERACT_WITH_CONTAINERS,
    INTERACT_WITH_DOORS,
    INTERACT_WITH_TRAPDOORS,
    INTERACT_WITH_FENCE_GATES,
    INTERACT_WITH_LEVERS,
    INTERACT_WITH_SIGNS,
    INTERACT_WITH_BOATS,
    INTERACT_WITH_MINECARTS,
    USE_TRIAL_MECHANICS,
    USE_FLUIDS,
    PRESS_BUTTONS,
    USE_PISTONS,
    USE_LEASHES,
    USE_FISHING_RODS,
    USE_FIRE_STARTERS,
    USE_FROST_WALKER,
    USE_SPAWN_EGGS,
    USE_SPONGES,
    EXTINGUISH_FIRE,
    THROW_POTIONS,
    THROW_ENDER_PEARLS,
    THROW_WIND_CHARGES,
    THROW_SNOWBALLS,
    THROW_EGGS,
    EAT_CHORUS_FRUITS,
    CREATE_NETHER_PORTALS,
    CAUSE_BLOCK_FALL,
    SPREAD_SCULK,
    PUSH_ENTITIES;

    public Text getName() {
        return Text.translatable("privit.rule." + this.name().toLowerCase() + ".name");
    }

    public Text getDescription() {
        return Text.translatable("privit.rule." + this.name().toLowerCase() + ".description");
    }
}