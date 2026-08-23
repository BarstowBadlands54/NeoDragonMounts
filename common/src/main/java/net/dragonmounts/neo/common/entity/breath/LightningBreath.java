package net.dragonmounts.neo.common.entity.breath;

import net.dragonmounts.neo.common.entity.dragon.TameableDragonEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

/**
 * Marks a breath that is drawn as real lightning geometry rather than as billboarded breath
 * particles.
 * <p>
 * This is an interface and not a base class on purpose: storm has to keep inheriting
 * {@link net.dragonmounts.neo.common.entity.breath.impl.WaterBreath WaterBreath}'s block
 * handling, so lightning-ness cannot live in the superclass chain. The client renderer only
 * needs the colour and the bolt count, and both breeds can supply those from wherever they
 * happen to sit in the hierarchy.
 *
 * @see net.dragonmounts.neo.common.client.renderer.breath.LightningBeamRenderer
 */
public interface LightningBreath {

    int STORM_COLOR = 0xE8F2FF;
    int MOONLIGHT_COLOR = 0x4C7BFF;
    int SUNLIGHT_COLOR = 0xFF8A1E;

    float WET_DAMAGE_MULTIPLIER = 1.5F;

    int getLightningColor();

    default int getBoltCount() {
        return 2;
    }

    default int getIgniteTicks() {
        return 100;
    }

    /**
     * Damage plus fire, shared by every lightning breed.
     * <p>
     * Note the interaction with {@link #WET_DAMAGE_MULTIPLIER}: a wet target takes more damage
     * but the fire is snuffed out on its next tick, so soaking in water trades burning for a
     * bigger hit rather than escaping both.
     *
     * @param damage already scaled by hit density at the call site
     */
    static void shock(
            ServerLevel level,
            TameableDragonEntity dragon,
            LivingEntity target,
            float damage,
            int igniteTicks
    ) {
        if (target.isInWaterOrRain()) {
            damage *= WET_DAMAGE_MULTIPLIER;
        }
        target.igniteForTicks(igniteTicks);
        target.hurt(level.damageSources().mobAttack(dragon), damage);
    }
}
