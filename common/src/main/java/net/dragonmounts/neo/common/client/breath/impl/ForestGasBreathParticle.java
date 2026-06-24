package net.dragonmounts.neo.common.client.breath.impl;

import net.dragonmounts.neo.common.client.breath.BreathParticle;
import net.dragonmounts.neo.common.client.breath.BreathParticleFactory;
import net.dragonmounts.neo.common.entity.breath.BreathAffectedEntity;
import net.dragonmounts.neo.common.entity.breath.BreathParticleOption;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

public class ForestGasBreathParticle extends FlameBreathParticle {
    public static final BreathParticleFactory FACTORY = IceBreathParticle::new;

    public ForestGasBreathParticle(BreathParticleOption option, TextureAtlasSprite sprite, ClientLevel level, double x, double y, double z, double motionX, double motionY, double motionZ) {
        super(option, sprite, level, x, y, z, motionX, motionY, motionZ);
    }

    @Override
    protected void tickIfAlive() {
        if (this.shouldExtinguish()) {
            this.level.addParticle(ParticleTypes.HAPPY_VILLAGER, this.x, this.y, this.z, 0, 0, 0);
        } else if (this.random.nextFloat() <= NORMAL_PARTICLE_CHANCE && this.random.nextFloat() < this.node.getLifetimeFraction()) {
            this.level.addParticle(ParticleTypes.HAPPY_VILLAGER, this.x, this.y, this.z, this.xd * 0.5, this.yd * 0.5, this.zd * 0.5);
        }
    }

    @Override
    protected ParticleOptions getChildParticle() {
        return ParticleTypes.HAPPY_VILLAGER;
    }
}