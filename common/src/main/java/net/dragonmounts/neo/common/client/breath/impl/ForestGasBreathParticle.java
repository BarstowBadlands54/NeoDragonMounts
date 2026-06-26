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

public class ForestGasBreathParticle extends BreathParticle {
    public static final BreathParticleFactory FACTORY = ForestGasBreathParticle::new;

    public ForestGasBreathParticle(BreathParticleOption option, TextureAtlasSprite sprite, ClientLevel level, double x, double y, double z, double motionX, double motionY, double motionZ) {
        super(option, sprite, level, x, y, z, motionX, motionY, motionZ);
    }

    @Override
    protected void tickIfAlive() {
        if (this.random.nextFloat() <= NORMAL_PARTICLE_CHANCE && this.random.nextFloat() < this.node.getLifetimeFraction()) {
            this.level.addParticle(
                    ParticleTypes.HAPPY_VILLAGER,
                    this.x + (this.random.nextFloat() * 2.0F - 1.0F) * this.bbWidth * 0.5F,
                    this.y + 0.8F,
                    this.z + (this.random.nextFloat() * 2.0F - 1.0F) * this.bbWidth * 0.5F,
                    this.xd,
                    this.yd,
                    this.zd
            );
        }
    }
}