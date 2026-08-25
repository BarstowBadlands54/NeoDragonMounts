package net.dragonmounts.neo.common.client.breath;

import net.dragonmounts.neo.common.client.ClientDragonEntity;
import net.dragonmounts.neo.common.util.math.MathUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

public class BreathSound extends AbstractTickableSoundInstance {
    public final ClientDragonEntity dragon;
    public boolean timeout;
    public BreathSound next;
    /** Set once the sound has actually been given a channel; see BreathSoundHandler#update. */
    public boolean acquired;
    public int retries;

    public BreathSound(ClientDragonEntity dragon, SoundEvent event, boolean looping) {
        super(event, SoundSource.NEUTRAL, SoundInstance.createUnseededRandom());
        this.dragon = dragon;
        this.looping = looping;
        this.delay = 0;
        this.reposition();
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public boolean canPlaySound() {
        return !this.dragon.isSilent();
    }

    @Override
    public void tick() {
        if (!this.dragon.isRemoved()) {
            this.reposition();
            if (this.volume > 0.01F) return;
        }
        this.stop();
    }

    private void reposition() {
        var pos = this.dragon.getHeadRelativeOffset(0.0F, -8.0F, 22.0F);
        this.x = pos.x;
        this.y = pos.y;
        this.z = pos.z;
        if (this.timeout) {
            this.volume *= 0.5F;
        } else {
            // Measured from the camera, not the player: OpenAL's listener is the camera
            // (SoundEngine#updateSource), and F5 moves one without the other, so a curve
            // anchored on the player is blind to the view mode.
            var camera = Minecraft.getInstance().gameRenderer.getMainCamera();
            this.volume = camera.isInitialized()
                    ? this.dragon.getAgeScale() * (1.0F - MathUtil.clamp((float) pos.distanceTo(camera.getPosition()) / 40.0F))
                    : 0.0F;
        }
    }

    public static class Scheduled extends BreathSound {
        public int duration;

        public Scheduled(ClientDragonEntity dragon, SoundEvent event, int duration) {
            super(dragon, event, false);
            this.duration = duration;
        }

        @Override
        public void tick() {
            if (--this.duration <= 0) {
                this.timeout = true;
            }
            super.tick();
        }
    }
}
