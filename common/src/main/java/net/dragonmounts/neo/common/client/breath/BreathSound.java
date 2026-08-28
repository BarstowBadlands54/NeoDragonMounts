package net.dragonmounts.neo.common.client.breath;

import net.dragonmounts.neo.common.client.ClientDragonEntity;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

public class BreathSound extends AbstractTickableSoundInstance {
    /**
     * Widens the audible radius without changing how loud the sound is up close.
     * <p>
     * SoundEngine clamps gain to 1, so a volume above that adds nothing at the muzzle, but the
     * attenuation distance it hands OpenAL is {@code max(volume, 1) * attenuation_distance}.
     * Against the 40 declared in sounds.json this reaches 60 blocks.
     */
    private static final float RANGE_BOOST = 1.5F;
    /// Floor, so a hatchling stays audible instead of tripping the fade-out test in tick().
    private static final float MIN_VOLUME = 0.35F;

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
            // Distance is the sound engine's job, not ours. This used to multiply in its own
            // 1 - distance/40 curve on top of the attenuation_distance of 40 already declared in
            // sounds.json, so the two stacked and loudness fell off as the square of the range.
            // Third person suffered most, the camera sitting further from the head than a rider
            // does. Size still scales it, so a hatchling stays quieter than an adult.
            this.volume = Math.max(this.dragon.getAgeScale() * RANGE_BOOST, MIN_VOLUME);
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
