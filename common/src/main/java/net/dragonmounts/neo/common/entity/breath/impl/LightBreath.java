package net.dragonmounts.neo.common.entity.breath.impl;

import net.dragonmounts.neo.common.entity.breath.BreathAffectedBlock;
import net.dragonmounts.neo.common.entity.breath.BreathAffectedEntity;
import net.dragonmounts.neo.common.entity.breath.DragonBreath;
import net.dragonmounts.neo.common.entity.breath.LightningBreath;
import net.dragonmounts.neo.common.entity.dragon.DragonLifeStage;
import net.dragonmounts.neo.common.entity.dragon.TameableDragonEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;

/// A pure arc: no terrain effect at all, only the strike. Subclasses supply the profile.
public abstract class LightBreath extends DragonBreath implements LightningBreath {
    public LightBreath(TameableDragonEntity dragon, float damage) {
        super(dragon, damage);
    }

    @Override
    public BreathAffectedBlock affectBlock(ServerLevel level, long location, BreathAffectedBlock hit) {
        return hit;
    }

    @Override
    public void affectEntity(ServerLevel level, LivingEntity target, BreathAffectedEntity hit) {
        this.strike(level, this.dragon, this.damage, target, hit);
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
