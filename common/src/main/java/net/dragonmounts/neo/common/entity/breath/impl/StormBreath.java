package net.dragonmounts.neo.common.entity.breath.impl;

import net.dragonmounts.neo.common.entity.breath.BreathAffectedBlock;
import net.dragonmounts.neo.common.entity.breath.BreathAffectedEntity;
import net.dragonmounts.neo.common.entity.breath.LightningBreath;
import net.dragonmounts.neo.common.entity.dragon.DragonLifeStage;
import net.dragonmounts.neo.common.entity.dragon.TameableDragonEntity;
import net.dragonmounts.neo.common.init.DMSounds;
import net.dragonmounts.neo.config.ServerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Storm: water breath's terrain handling with a lightning arc on top.
 * <p>
 * It still extends {@link WaterBreath}, so everything the water breath does to blocks -- laying
 * down draining puddles, quenching lava, watering farmland, snuffing candles -- happens here
 * unchanged. What is added is the arc: mobs are shocked and set alight rather than doused, and
 * the beam occasionally calls down a real {@link EntityType#LIGHTNING_BOLT} where it lands.
 * <p>
 * Note the deliberate asymmetry. Blocks get wet, mobs get burnt. A mob standing in the puddle
 * the breath just laid down will be extinguished on its next tick, which is the intended
 * interaction rather than an oversight: fire is the reward for catching a dry target.
 */
public class StormBreath extends WaterBreath implements LightningBreath {
    /// Minimum ticks between two summoned bolts. affectBlock runs once per struck block per
    /// tick, so a per-call random with no floor would call down a wall of lightning.
    protected static final int STRIKE_INTERVAL = 20;
    /// Per-block chance to roll a strike, once the interval above has elapsed.
    protected static final float STRIKE_CHANCE = 0.06F;
    /// Bolts damage a 3x3 column, so keep them off the dragon and its rider.
    protected static final double MIN_STRIKE_DISTANCE = 5.0;

    /// Game time the next bolt becomes available. Instance state: one breath per dragon.
    private long nextStrikeTime;

    public StormBreath(TameableDragonEntity dragon, float damage) {
        super(dragon, damage);
    }

    @Override
    public int getLightningColor() {
        return STORM_COLOR;
    }

    @Override
    public int getBoltCount() {
        return 3;
    }

    @Override
    public BreathAffectedBlock affectBlock(ServerLevel level, long location, BreathAffectedBlock hit) {
        this.tryStrike(level, BlockPos.of(location));
        return super.affectBlock(level, location, hit);
    }

    /**
     * Shock and ignite instead of {@link WaterBreath}'s dousing. The knockback matches the water
     * breath's so the two breeds still feel related in the hand.
     */
    @Override
    public void affectEntity(ServerLevel level, LivingEntity target, BreathAffectedEntity hit) {
        float density = hit.getHitDensity();
        LightningBreath.shock(level, this.dragon, target, this.damage * density, this.getIgniteTicks());
        var direction = hit.getHitDirection();
        target.knockback(0.05F * density, -direction.x, -direction.z);
    }

    /**
     * Occasionally call down a real bolt on a block the beam is washing over.
     * <p>
     * The bolt is a genuine entity, so it does everything vanilla lightning does: converts pigs
     * to zombified piglins, charges creepers, powers lightning rods, deoxidises copper, and
     * feeds {@code onThunderHit} -- which for a storm dragon standing in its own beam means the
     * Strength buff from {@link net.dragonmounts.neo.common.type.StormType}. Fires respect the
     * {@code doFireTick} gamerule on their own; the visual-only fallback is for servers that
     * have turned off igniting breath entirely.
     *
     * @return true if a bolt was summoned
     */
    protected boolean tryStrike(ServerLevel level, BlockPos pos) {
        long now = level.getGameTime();
        if (now < this.nextStrikeTime || level.random.nextFloat() >= STRIKE_CHANCE) return false;
        if (this.dragon.distanceToSqr(Vec3.atCenterOf(pos)) < MIN_STRIKE_DISTANCE * MIN_STRIKE_DISTANCE) {
            return false;
        }
        var bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt == null) return false;
        this.nextStrikeTime = now + STRIKE_INTERVAL;
        bolt.moveTo(Vec3.atBottomCenterOf(pos.above()));
        bolt.setVisualOnly(!ServerConfig.INSTANCE.ignitingBreath.get());
        // Kill credit follows the owner, so a tamed dragon's strikes read as the player's.
        if (this.dragon.getOwner() instanceof ServerPlayer owner) {
            bolt.setCause(owner);
        }
        level.addFreshEntity(bolt);
        return true;
    }

    // Storm inherited the ice breath's sounds as a placeholder; it has its own now.

    @Override
    public SoundEvent getStartSound(DragonLifeStage stage) {
        return DMSounds.DRAGON_BREATH_START_LIGHTNING;
    }

    @Override
    public SoundEvent getLoopSound(DragonLifeStage stage) {
        return DMSounds.DRAGON_BREATH_LOOP_LIGHTNING;
    }

    @Override
    public SoundEvent getStopSound(DragonLifeStage stage) {
        return DMSounds.DRAGON_BREATH_STOP_LIGHTNING;
    }
}
