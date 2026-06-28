package net.dragonmounts.neo.common.entity.breath.impl;

import net.dragonmounts.neo.common.entity.breath.BreathAffectedBlock;
import net.dragonmounts.neo.common.entity.breath.BreathAffectedEntity;
import net.dragonmounts.neo.common.entity.breath.DragonBreath;
import net.dragonmounts.neo.common.entity.dragon.DragonLifeStage;
import net.dragonmounts.neo.common.entity.dragon.TameableDragonEntity;
import net.dragonmounts.neo.common.init.DMSounds;
import net.dragonmounts.neo.config.ServerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.core.Direction;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.MOISTURE;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.LEVEL;

public class WaterBreath extends DragonBreath {
    public WaterBreath(TameableDragonEntity dragon, float damage) {
        super(dragon, damage);
    }

    @Override
    public BreathAffectedBlock affectBlock(ServerLevel level, long location, BreathAffectedBlock hit) {
        var pos = BlockPos.of(location);
        var state = level.getBlockState(pos);
        if (state.is(Blocks.LAVA)) {
            if (!ServerConfig.INSTANCE.quenchingBreath.get()) return hit;
            level.setBlockAndUpdate(pos, (
                    level.getFluidState(pos).isSource() ? Blocks.OBSIDIAN : Blocks.COBBLESTONE
            ).defaultBlockState());
            level.levelEvent(1501, pos, 0);
        } else if (AbstractCandleBlock.isLit(state)) {
            AbstractCandleBlock.extinguish(null, state, level, pos);
        } else if (CampfireBlock.isLitCampfire(state)) {
            level.levelEvent(null, 1009, pos, 0);
            level.setBlockAndUpdate(pos, state.setValue(CampfireBlock.LIT, false));
        } else if (state.is(BlockTags.FIRE)) {
            level.destroyBlock(pos, true, this.dragon);
        } else if (state.hasProperty(MOISTURE)) {
            int moisture = state.getValue(MOISTURE);
            if (moisture >= FarmBlock.MAX_MOISTURE) return hit;
            level.setBlock(pos, state.setValue(MOISTURE, moisture + 1), 2);
        } else {
            // Lay down temporary "spreading water": flowing water (never a source) on the ground
            // where the breath lands. It drains on its own because nothing feeds it, leaving no
            // permanent water behind.
            this.spreadTemporaryWater(level, pos);
        }
        return new BreathAffectedBlock(); // reset to zero
    }

    /**
     * Place short-lived FLOWING water (level 1) on an empty spot above solid ground, then schedule
     * a fluid tick so vanilla's flowing-fluid logic spreads and then drains it (no source feeds it,
     * so it recedes on its own). We only ever place flowing water — never a source — so it can't
     * leave a permanent pool. The puddle that spreads out is exactly the thinning "water layers"
     * that disappear once their source is gone.
     */
    private void spreadTemporaryWater(ServerLevel level, BlockPos hitPos) {
        // find a spot at the hit, or just above/below it, that is currently empty and can hold water
        BlockPos place = findWaterSpot(level, hitPos);
        if (place == null) return;

        // flowing water at level 1 (strongest flow, still not a source)
        level.setBlock(place, Blocks.WATER.defaultBlockState().setValue(LEVEL, 1), 3);
        // tick the fluid so it immediately starts spreading/draining instead of sitting static
        level.scheduleTick(place, Fluids.WATER, 5);
    }

    /** Return a nearby empty, water-holding position (the hit, the block above, or below), or null. */
    private BlockPos findWaterSpot(ServerLevel level, BlockPos hitPos) {
        // prefer the air block just above the surface the breath struck, then the hit itself
        for (BlockPos candidate : new BlockPos[]{hitPos.above(), hitPos, hitPos.below()}) {
            var state = level.getBlockState(candidate);
            // the spot must currently hold no fluid and be air/replaceable (don't overwrite blocks)
            if (!level.getFluidState(candidate).isEmpty()) continue;
            if (!state.isAir() && !state.canBeReplaced()) continue;
            // and it needs something to rest on so the puddle doesn't just fall forever
            var below = level.getBlockState(candidate.below());
            boolean groundBelow = below.isFaceSturdy(level, candidate.below(), Direction.UP)
                    || !below.getFluidState().isEmpty();
            if (groundBelow) return candidate;
        }
        return null;
    }

    @Override
    public void affectEntity(ServerLevel level, LivingEntity target, BreathAffectedEntity hit) {
        float density = hit.getHitDensity();
        float damage = this.damage * hit.getHitDensity();
        if (target.getType().is(EntityTypeTags.AQUATIC)) damage += 4;
        if (target.canBreatheUnderwater() || MobEffectUtil.hasWaterBreathing(target)) damage *= 0.5F;
        if (target.isInPowderSnow) {
            target.setTicksFrozen(Math.min(target.getTicksRequiredToFreeze(), target.getTicksFrozen() + 2));
        }
        if (target.isOnFire()) {
            target.clearFire();
            target.playSound(SoundEvents.GENERIC_EXTINGUISH_FIRE, 1.0f, 0.0f);
            damage *= 1.5F;
        } else {
            target.playSound(SoundEvents.GENERIC_SPLASH, 0.4f, 1.0f);
        }
        target.hurt(level.damageSources().mobAttack(this.dragon), damage);
        var direction = hit.getHitDirection();
        target.knockback(0.05F * density, -direction.x, -direction.z);
    }

    @Override
    public SoundEvent getStartSound(DragonLifeStage stage) {
        return DMSounds.DRAGON_BREATH_START_WATER;
    }

    @Override
    public SoundEvent getLoopSound(DragonLifeStage stage) {
        return DMSounds.DRAGON_BREATH_LOOP_WATER;
    }

    @Override
    public SoundEvent getStopSound(DragonLifeStage stage) {
        return DMSounds.DRAGON_BREATH_STOP_WATER;
    }

}