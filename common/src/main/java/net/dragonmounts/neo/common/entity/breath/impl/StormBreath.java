package net.dragonmounts.neo.common.entity.breath.impl;

import net.dragonmounts.neo.common.entity.breath.BreathAffectedBlock;
import net.dragonmounts.neo.common.entity.breath.BreathAffectedEntity;
import net.dragonmounts.neo.common.entity.breath.LightningBreath;
import net.dragonmounts.neo.common.entity.breath.LightningProfile;
import net.dragonmounts.neo.common.entity.dragon.DragonLifeStage;
import net.dragonmounts.neo.common.entity.dragon.TameableDragonEntity;
import net.dragonmounts.neo.config.ServerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Water breath's terrain handling with a lightning arc on top. It still extends
 * {@link WaterBreath}, so the puddles, quenched lava, watered farmland and snuffed candles all
 * happen here unchanged; what is added is the strike, plus the occasional real
 * {@link EntityType#LIGHTNING_BOLT} called down where the beam lands.
 * <p>
 * Blocks get wet and mobs get burnt, which is deliberate rather than an oversight: a mob standing
 * in the puddle the breath just laid down is extinguished on its next tick, so fire is the reward
 * for catching a dry target.
 */
public class StormBreath extends WaterBreath implements LightningBreath {
    /// Minimum ticks between two summoned bolts. affectBlock runs once per struck block per tick,
    /// so a per-call random with no floor would call down a wall of lightning.
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
    public LightningProfile getLightningProfile() {
        return LightningProfile.STORM;
    }

    @Override
    public BreathAffectedBlock affectBlock(ServerLevel level, long location, BreathAffectedBlock hit) {
        this.tryStrike(level, BlockPos.of(location));
        return super.affectBlock(level, location, hit);
    }

    /// Shock and ignite instead of {@link WaterBreath}'s dousing.
    @Override
    public void affectEntity(ServerLevel level, LivingEntity target, BreathAffectedEntity hit) {
        this.strike(level, this.dragon, this.damage, target, hit);
    }

    /**
     * Occasionally call down a real bolt on a block the beam is washing over. The bolt is a
     * genuine entity, so it does everything vanilla lightning does: converts pigs, charges
     * creepers, powers rods, deoxidises copper, and feeds {@code onThunderHit} -- which for a
     * storm dragon standing in its own beam is the Strength buff from
     * {@link net.dragonmounts.neo.common.type.StormType}. The visual-only fallback is for servers
     * that have turned igniting breath off entirely.
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
        // kill credit follows the owner, so a tamed dragon's strikes read as the player's
        if (this.dragon.getOwner() instanceof ServerPlayer owner) {
            bolt.setCause(owner);
        }
        level.addFreshEntity(bolt);
        return true;
    }

    @Override
    public SoundEvent getStartSound(DragonLifeStage stage) {
        return this.getLightningStartSound();
    }

    @Override
    public SoundEvent getLoopSound(DragonLifeStage stage) {
        return this.getLightningLoopSound();
    }

    @Override
    public SoundEvent getStopSound(DragonLifeStage stage) {
        return this.getLightningStopSound();
    }
}
