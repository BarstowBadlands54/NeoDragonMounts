package net.dragonmounts.neo.common.entity.breath;

import net.dragonmounts.neo.common.entity.dragon.TameableDragonEntity;
import net.dragonmounts.neo.common.init.DMSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;

/**
 * A breath drawn as real lightning geometry rather than as billboarded breath particles, and
 * carrying the strike that goes with it.
 * <p>
 * An interface and not a base class on purpose: storm has to keep inheriting
 * {@link net.dragonmounts.neo.common.entity.breath.impl.WaterBreath WaterBreath}'s block
 * handling, so lightning-ness cannot live in the superclass chain. Everything shared is a
 * default here; implementors only supply a {@link LightningProfile} and forward the two members
 * {@link DragonBreath} declares as class methods, which no interface default can override.
 *
 * @see net.dragonmounts.neo.common.client.renderer.breath.LightningBeamRenderer
 */
public interface LightningBreath {
    /// Knockback matches WaterBreath's, so the breeds still feel related in the hand.
    float KNOCKBACK = 0.05F;

    LightningProfile getLightningProfile();

    /**
     * Damage, ignite and knockback, shared by every lightning breed. Forward
     * {@link DragonBreath#affectEntity} to this.
     *
     * @param damage the breath's base damage, before hit density is applied
     */
    default void strike(
            ServerLevel level,
            TameableDragonEntity dragon,
            float damage,
            LivingEntity target,
            BreathAffectedEntity hit
    ) {
        var profile = this.getLightningProfile();
        float density = hit.getHitDensity();
        float dealt = damage * density;
        if (target.isInWaterOrRain()) {
            dealt *= profile.wetDamageMultiplier();
        }
        target.igniteForTicks(profile.igniteTicks());
        target.hurt(level.damageSources().mobAttack(dragon), dealt);
        var direction = hit.getHitDirection();
        target.knockback(KNOCKBACK * density, -direction.x, -direction.z);
    }

    /// One set of sounds for every breed and life stage: a hatchling's arc crackles like an
    /// adult's, and BreathSound already scales volume by age.
    default SoundEvent getLightningStartSound() {
        return DMSounds.DRAGON_BREATH_START_LIGHTNING;
    }

    default SoundEvent getLightningLoopSound() {
        return DMSounds.DRAGON_BREATH_LOOP_LIGHTNING;
    }

    default SoundEvent getLightningStopSound() {
        return DMSounds.DRAGON_BREATH_STOP_LIGHTNING;
    }
}
