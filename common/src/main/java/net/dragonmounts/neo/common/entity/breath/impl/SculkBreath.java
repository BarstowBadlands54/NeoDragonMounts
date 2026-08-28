package net.dragonmounts.neo.common.entity.breath.impl;

import net.dragonmounts.neo.common.entity.breath.BreathAffectedBlock;
import net.dragonmounts.neo.common.entity.breath.BreathAffectedEntity;
import net.dragonmounts.neo.common.entity.breath.DragonBreath;
import net.dragonmounts.neo.common.entity.dragon.DragonLifeStage;
import net.dragonmounts.neo.common.entity.dragon.TameableDragonEntity;
import net.dragonmounts.neo.common.init.DMSounds;
import net.dragonmounts.neo.config.ServerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Sculk breath. It seeds sculk rather than laying it down: a burst leaves a block or two where
 * the beam dwelt, not a carpet over everything it swept across.
 * <p>
 * Three things hold the rate down. A block has to soak in the stream before it is a candidate at
 * all, so a passing sweep leaves the ground alone. Conversions are then spaced by a hard interval,
 * because {@code affectBlock} runs once per struck block per tick and there can be hundreds of
 * those. The chance roll on top only decides which of the soaked blocks goes, so the patch grows
 * unevenly instead of always from the same spot.
 */
public class SculkBreath extends DragonBreath {
    /**
     * Hit density a block must reach before it can convert.
     * <p>
     * Density accrues per tick of exposure and decays once the beam moves off, so this is a dwell
     * time in disguise. For scale, {@link FireBreath} smelts at 0.5 and sets a solid face alight
     * at 6.0; sculk wants a deliberate hold, not a brush.
     */
    private static final float SPREAD_THRESHOLD = 2.0F;
    /// Minimum ticks between two conversions, whatever else the beam is touching.
    private static final int SPREAD_INTERVAL = 20;
    /// Per-block chance to convert, once the interval above has elapsed.
    private static final float SPREAD_CHANCE = 0.5F;
    /// Chance a conversion plants a catalyst instead of plain sculk.
    private static final float CATALYST_CHANCE = 0.02F;
    /// Chance a plain conversion also grows a vein on the block above.
    private static final float VEIN_CHANCE = 0.08F;
    private static final int DARKNESS_TICKS = 80;
    private static final float DARKNESS_THRESHOLD = 0.2F;

    /// Game time the next conversion becomes available. Instance state: one breath per dragon.
    private long nextSpreadTime;

    public SculkBreath(TameableDragonEntity dragon, float damage) {
        super(dragon, damage);
    }

    @Override
    public BreathAffectedBlock affectBlock(ServerLevel level, long location, BreathAffectedBlock hit) {
        if (!ServerConfig.INSTANCE.destructiveBreath.get()) return hit;
        if (hit.getMaxHitDensity() < SPREAD_THRESHOLD) return hit;

        long now = level.getGameTime();
        var random = level.random;
        if (now < this.nextSpreadTime || random.nextFloat() >= SPREAD_CHANCE) return hit;

        var pos = BlockPos.of(location);
        if (!level.getBlockState(pos).is(BlockTags.SCULK_REPLACEABLE)) return hit;

        this.nextSpreadTime = now + SPREAD_INTERVAL;
        boolean catalyst = random.nextFloat() < CATALYST_CHANCE;
        level.setBlockAndUpdate(pos, (catalyst ? Blocks.SCULK_CATALYST : Blocks.SCULK).defaultBlockState());
        level.sendParticles(ParticleTypes.SCULK_SOUL,
                pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                catalyst ? 6 : 2, 0.25, 0.1, 0.25, 0.0);

        if (!catalyst && random.nextFloat() < VEIN_CHANCE) {
            var above = pos.above();
            if (level.getBlockState(above).isAir()) {
                level.setBlockAndUpdate(above, Blocks.SCULK_VEIN.defaultBlockState()
                        .setValue(BlockStateProperties.DOWN, true));
            }
        }
        // spent: the block starts soaking again from zero, though as sculk it no longer qualifies
        return new BreathAffectedBlock();
    }

    @Override
    public void affectEntity(ServerLevel level, LivingEntity target, BreathAffectedEntity hit) {
        float density = hit.getHitDensity();
        target.hurt(level.damageSources().mobAttack(this.dragon), this.damage * density);
        if (density > DARKNESS_THRESHOLD) {
            target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, DARKNESS_TICKS), this.dragon);
        }
        var direction = hit.getHitDirection();
        target.knockback(0.05F * density, -direction.x, -direction.z);
    }

    @Override
    public SoundEvent getStartSound(DragonLifeStage stage) {
        return DMSounds.DRAGON_BREATH_START_SCULK;
    }

    @Override
    public SoundEvent getLoopSound(DragonLifeStage stage) {
        return DMSounds.DRAGON_BREATH_LOOP_SCULK;
    }

    @Override
    public SoundEvent getStopSound(DragonLifeStage stage) {
        return DMSounds.DRAGON_BREATH_STOP_SCULK;
    }
}
