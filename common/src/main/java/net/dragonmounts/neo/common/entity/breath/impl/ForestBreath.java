package net.dragonmounts.neo.common.entity.breath.impl;

import net.dragonmounts.neo.common.entity.breath.BreathAffectedBlock;
import net.dragonmounts.neo.common.entity.breath.BreathAffectedEntity;
import net.dragonmounts.neo.common.entity.breath.DragonBreath;
import net.dragonmounts.neo.common.entity.dragon.DragonLifeStage;
import net.dragonmounts.neo.common.entity.dragon.TameableDragonEntity;
import net.dragonmounts.neo.common.init.DMSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.UseBonemeal;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.level.block.BonemealableBlock;

public class ForestBreath extends DragonBreath {
    public ForestBreath(TameableDragonEntity dragon, float damage) {
        super(dragon, damage);
    }

    @Override
    public BreathAffectedBlock affectBlock(ServerLevel level, long location, BreathAffectedBlock hit) {
        var pos = BlockPos.of(location).above();
        if (!level.getBlockState(pos).isAir() && level.random.nextFloat() < 0.002F) {
            var cloud = createEffectCloud(level, pos, 1.3F, 600);
            cloud.setOwner(this.dragon);
            cloud.addEffect(new MobEffectInstance(MobEffects.POISON, 100));
            level.addFreshEntity(cloud);
        }

        growPlant(level, pos);
        return new BreathAffectedBlock(); // reset to zero

    }

    private void growPlant(ServerLevel level, BlockPos pos) {
        var state = level.getBlockState(pos);
        var block = state.getBlock();

        if (block instanceof BonemealableBlock growable) {
            if (growable.isValidBonemealTarget(level, pos, state)) {
                if (growable.isBonemealSuccess(level, level.random, pos, state)) {
                    growable.performBonemeal(level, level.random, pos, state);

                    // Vanilla bonemeal event (green swirl)
                    level.levelEvent(2005, pos, 0);

                    // Add your own custom growth sparkle or leaf particles
                    spawnGrowthParticles(level, pos);
                }
            }
        }
    }

    private void spawnGrowthParticles(ServerLevel level, BlockPos pos) {
        var random = level.random;
        for (int i = 0; i < 8; i++) {
            double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5);
            double y = pos.getY() + random.nextDouble();
            double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5);

            // Velocity for a gentle floating effect
            double dx = (random.nextDouble() - 0.5) * 0.02;
            double dy = random.nextDouble() * 0.02;
            double dz = (random.nextDouble() - 0.5) * 0.02;

            // You can use a custom particle here if registered
            level.sendParticles(
                    ParticleTypes.HAPPY_VILLAGER, // or your custom particle
                    x, y, z,
                    1, dx, dy, dz, 0.0
            );
        }
    }

    @Override
    public void affectEntity(ServerLevel level, LivingEntity target, BreathAffectedEntity hit) {
        // Only damage hostile mobs or players
        if (!isHostileOrPlayer(target)) {
            return;
        }

        target.hurt(
                level.damageSources().mobAttack(this.dragon),
                this.damage * hit.getHitDensity()
        );
    }

    private boolean isHostileOrPlayer(LivingEntity entity) {
        // Hostile mobs (zombies, skeletons, pillagers, etc.)
        if (entity instanceof Enemy) {
            return true;
        }

        // fish
        if (entity instanceof WaterAnimal) {
            return false;
        }

        // villagers
        if (entity instanceof AbstractVillager) {
            return false;
        }

        // Golems
        if(entity instanceof AbstractGolem) {
            return false;
        }


        // Everything else (animals, villagers, etc.) is safe
        return true;
    }

    @Override
    public SoundEvent getStartSound(DragonLifeStage stage) {
        return DMSounds.DRAGON_BREATH_START_FOREST;
    }

    @Override
    public SoundEvent getLoopSound(DragonLifeStage stage) {
        return DMSounds.DRAGON_BREATH_LOOP_FOREST;
    }

    @Override
    public SoundEvent getStopSound(DragonLifeStage stage) {
        return DMSounds.DRAGON_BREATH_STOP_FOREST;
    }
}
