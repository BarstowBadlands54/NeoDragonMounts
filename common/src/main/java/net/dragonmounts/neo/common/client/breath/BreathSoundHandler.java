package net.dragonmounts.neo.common.client.breath;

import net.minecraft.client.Minecraft;

public class BreathSoundHandler {
    /** How long to keep retrying a sound that could not get a channel, in ticks. */
    private static final int MAX_RETRY_TICKS = 20;

    private BreathSound playing;

    public void update() {
        var manager = Minecraft.getInstance().getSoundManager();
        var sound = this.playing;
        while (sound != null) {
            if (manager.isActive(sound)) {
                sound.acquired = true;
                if (!sound.timeout) return;
            } else if (!sound.acquired && sound.retries++ < MAX_RETRY_TICKS) {
                // The sound never got a channel: the static pool was full when play() ran, so
                // SoundEngine returned without registering it. isActive() reports that the same
                // way as "finished playing", so retry rather than advancing past the one-shots.
                manager.play(sound);
                return;
            }
            this.playing = sound = sound.next;
            if (sound != null) {
                manager.play(sound);
            }
        }
    }

    public void play(BreathSound sound) {
        this.setTimeout();
        Minecraft.getInstance().getSoundManager().play(this.playing = sound);
    }

    public void setTimeout() {
        var playing = this.playing;
        if (playing != null) {
            playing.timeout = true;
        }
    }
}
