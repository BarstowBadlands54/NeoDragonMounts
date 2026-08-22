package net.dragonmounts.neo.common.entity.ai.control;

import net.dragonmounts.neo.common.entity.dragon.TameableDragonEntity;
import net.minecraft.world.entity.ai.control.LookControl;

/**
 * Hands pitch and yaw to DragonBreathAttack for the duration of a breath.
 * <p>
 * Vanilla {@link LookControl#tick()} zeroes xRot every tick and re-derives it from LOOK_TARGET,
 * and it runs after the brain -- so it silently overwrote whatever firing solution
 * {@code faceTarget} had just computed. Two things went wrong as a result. LOOK_TARGET is an
 * {@code EntityTracker(target, true)}, which reports the target's EYES, so a shot from above was
 * aimed roughly a block over the target's head and, at a shallow depression angle, landed a good
 * way past it -- the stream drawing a line behind whatever was running at the dragon. And any
 * lead correction was thrown away along with it.
 * <p>
 * Standing aside while breathing leaves exactly one writer for the aim. Outside a breath this is
 * vanilla behaviour untouched.
 */
public class DragonLookControl extends LookControl {
    private final TameableDragonEntity dragon;

    public DragonLookControl(TameableDragonEntity dragon) {
        super(dragon);
        this.dragon = dragon;
    }

    @Override
    public void tick() {
        // faceTarget already set yRot, yBodyRot, yHeadRot and xRot together this tick.
        if (this.dragon.isBreathing()) return;
        super.tick();
    }
}
