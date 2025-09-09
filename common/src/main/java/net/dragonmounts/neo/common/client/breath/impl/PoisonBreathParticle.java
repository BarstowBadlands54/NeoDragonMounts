package net.dragonmounts.neo.common.client.breath.impl;

import net.dragonmounts.neo.common.client.breath.BreathParticle;
import net.dragonmounts.neo.common.client.breath.BreathParticleFactory;
import net.dragonmounts.neo.common.entity.breath.BreathParticleOption;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

public class PoisonBreathParticle extends BreathParticle {
    public static final BreathParticleFactory FACTORY = PoisonBreathParticle::new;

    public PoisonBreathParticle(BreathParticleOption option, TextureAtlasSprite sprite, ClientLevel level, double x, double y, double z, double motionX, double motionY, double motionZ) {
        super(option, sprite, level, x, y, z, motionX, motionY, motionZ);
    }

    @Override
    protected void tickIfAlive() {}
}
