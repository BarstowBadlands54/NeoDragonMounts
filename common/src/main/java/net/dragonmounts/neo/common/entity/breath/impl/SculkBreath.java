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

public class SculkBreath extends DragonBreath {
    private static final float CATALYST_CHANCE = 0.0004F;
    private static final float VEIN_CHANCE = 0.08F;
    private static final int DARKNESS_TICKS = 80;
    private static final float DARKNESS_THRESHOLD = 0.2F;

    public SculkBreath(TameableDragonEntity dragon, float damage) {
        super(dragon, damage);
    }

    @Override
    public BreathAffectedBlock affectBlock(ServerLevel level, long location, BreathAffectedBlock hit) {
        if (!ServerConfig.INSTANCE.destructiveBreath.get()) return new BreathAffectedBlock();

        var pos = BlockPos.of(location);
        var state = level.getBlockState(pos);
        if (!state.is(BlockTags.SCULK_REPLACEABLE)) return new BreathAffectedBlock();

        var random = level.random;
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
