package net.dragonmounts.neo.common.entity.breath.impl;

import net.dragonmounts.neo.common.entity.breath.BreathAffectedBlock;
import net.dragonmounts.neo.common.entity.breath.BreathAffectedEntity;
import net.dragonmounts.neo.common.entity.breath.DragonBreath;
import net.dragonmounts.neo.common.entity.dragon.DragonLifeStage;
import net.dragonmounts.neo.common.entity.dragon.TameableDragonEntity;
import net.dragonmounts.neo.common.init.DMSounds;
import net.dragonmounts.neo.config.ServerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

public class IceBreath extends DragonBreath {
    public IceBreath(TameableDragonEntity dragon, float damage) {
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
        } else if (state.is(Blocks.WATER)) {
            if (!level.getFluidState(pos).is(Fluids.WATER)) return hit; // requires full water block
            level.setBlockAndUpdate(pos, (
                    ServerConfig.INSTANCE.frostyBreath.get() ? Blocks.ICE : Blocks.FROSTED_ICE
            ).defaultBlockState());
        } else if (AbstractCandleBlock.isLit(state)) {
            AbstractCandleBlock.extinguish(null, state, level, pos);
        } else if (CampfireBlock.isLitCampfire(state)) {
            level.levelEvent(null, 1009, pos, 0);
            level.setBlockAndUpdate(pos, state.setValue(CampfireBlock.LIT, false));
        } else if (state.is(BlockTags.FIRE)) {
            level.destroyBlock(pos, true, this.dragon);
        } else {
            if (!ServerConfig.INSTANCE.frostyBreath.get()) return new BreathAffectedBlock();

            BlockPos snowPos;
            if (state.canBeReplaced()) {
                // grass plant / fern / replaceable foliage -> snow REPLACES it at this position
                snowPos = pos;
            } else if (state.isAir()) {
                // air over ground -> snow goes here
                snowPos = pos;
            } else if (state.isFaceSturdy(level, pos, Direction.UP) || state.is(BlockTags.LEAVES)) {
                // solid block -> snow goes on top
                snowPos = pos.above();
            } else {
                return new BreathAffectedBlock();
            }

            // require solid support below the snow
            BlockState below = level.getBlockState(snowPos.below());
            if (!below.isFaceSturdy(level, snowPos.below(), Direction.UP) && !below.is(BlockTags.LEAVES)) {
                return new BreathAffectedBlock();
            }

            BlockState existing = level.getBlockState(snowPos);
            if (existing.is(Blocks.SNOW)) {
                int layers = existing.getValue(SnowLayerBlock.LAYERS);
                if (layers < SnowLayerBlock.MAX_HEIGHT) {
                    level.setBlockAndUpdate(snowPos, existing.setValue(SnowLayerBlock.LAYERS, layers + 1));
                }
            } else if (existing.isAir() || existing.canBeReplaced()) {   // ← also overwrite replaceable plants
                level.setBlockAndUpdate(snowPos, Blocks.SNOW.defaultBlockState());
            }
        }
        return new BreathAffectedBlock(); // reset to zero
    }

    @Override
    public void affectEntity(ServerLevel level, LivingEntity target, BreathAffectedEntity hit) {
        float density = hit.getHitDensity();
        float damage = this.damage * density;
        if (target.isOnFire()) {
            target.clearFire();
            target.playSound(SoundEvents.GENERIC_EXTINGUISH_FIRE, 1.0f, 0.0f);
            damage *= 2;
        }
        if (target.canFreeze()) {
            target.setTicksFrozen(Math.min(target.getTicksRequiredToFreeze(), target.isInPowderSnow
                    ? target.getTicksFrozen() + 1
                    : target.getTicksFrozen() + 3
            ));
        }
        target.hurt(level.damageSources().mobAttack(this.dragon), damage);
        var direction = hit.getHitDirection();
        target.knockback(0.075F * density, -direction.x, -direction.z);
    }

    @Override
    public SoundEvent getStartSound(DragonLifeStage stage) {
        return DMSounds.DRAGON_BREATH_START_ICE;
    }

    @Override
    public SoundEvent getLoopSound(DragonLifeStage stage) {
        return DMSounds.DRAGON_BREATH_LOOP_ICE;
    }

    @Override
    public SoundEvent getStopSound(DragonLifeStage stage) {
        return DMSounds.DRAGON_BREATH_STOP_ICE;
    }
}
