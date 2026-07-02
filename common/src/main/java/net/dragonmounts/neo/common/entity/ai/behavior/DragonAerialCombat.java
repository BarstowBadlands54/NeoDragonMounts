package net.dragonmounts.neo.common.entity.ai.behavior;

import net.dragonmounts.neo.common.entity.dragon.TameableDragonEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

/**
 * Aerial combat positioning for an UNRIDDEN dragon. This behavior does NOT breathe or bite — it
 * decides WHEN the dragon should take to the air during a fight and flies it into a good attack
 * position. DragonBreathAttack / MeleeAttack still handle the actual attacks; this just gets the
 * dragon airborne and positioned so those can connect.
 *
 * It engages from the air when:
 *   - the target is itself airborne (e.g. another flying dragon, a phantom) — our dragon must
 *     fly to fight it at all, OR
 *   - our dragon is hurt (health below HURT_FRACTION) against a ground target — taking to the air
 *     gives it a safer, breath-focused kiting position instead of trading bites on the ground.
 *
 * While engaged it holds an altitude above the target and stays within breath range, so
 * DragonBreathAttack (range 32) can fire. When the trigger no longer applies (target gone, or
 * healed up against a grounded foe) it releases, and the dragon settles back to ground melee.
 *
 * Typed on TameableDragonEntity to conform in the activity list like the other GoalBehaviors.
 */
public class DragonAerialCombat extends GoalBehavior<TameableDragonEntity> {
    /** Take to the air against a GROUND target once health drops below this fraction. */
    private static final float HURT_FRACTION = 0.70F;
    /** Return to ground combat against a ground target once healed back above this (hysteresis). */
    private static final float RECOVER_FRACTION = 0.85F;

    /** Engage from the air only while the target is within this horizontal-ish range. */
    private static final double ENGAGE_RANGE_SQR = 40.0 * 40.0;
    /** Desired height to hold above the target while strafing it. */
    private static final double STRAFE_HEIGHT = 6.0;
    /** Ideal standoff distance from the target (kept under DragonBreathAttack's 32-block range). */
    private static final double STANDOFF = 12.0;
    /** Flight speed multiplier for the move control. */
    private static final double FLIGHT_SPEED = 1.3;

    /** Latched once we commit to an aerial engagement, so health hysteresis works correctly. */
    private boolean committedAir = false;

    public DragonAerialCombat() {
    }

    @Override
    protected boolean canUse(ServerLevel level, TameableDragonEntity dragon) {
        if (dragon.isBeingRiddenByPlayer()) return false;
        if (dragon.isBaby()) return false;            // babies can't fly
        if (dragon.isInWater()) return false;

        LivingEntity target = dragon.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
        if (target == null || !target.isAlive()) {
            this.committedAir = false;
            return false;
        }
        if (dragon.distanceToSqr(target) > ENGAGE_RANGE_SQR) {
            this.committedAir = false;
            return false;
        }
        return this.shouldFly(dragon, target);
    }

    /** Decide whether this fight warrants being in the air. */
    private boolean shouldFly(TameableDragonEntity dragon, LivingEntity target) {
        // 1) airborne target -> we must fly to engage it
        if (this.isAirborne(target)) {
            this.committedAir = true;
            return true;
        }

        // 2) ground target + we're hurt -> take to the air. Hysteresis so we don't flap up and
        //    down right around the threshold: commit below HURT_FRACTION, release above RECOVER.
        float frac = dragon.getHealth() / dragon.getMaxHealth();
        if (this.committedAir) {
            if (frac >= RECOVER_FRACTION) {
                this.committedAir = false;
                return false;
            }
            return true;
        }
        if (frac < HURT_FRACTION) {
            this.committedAir = true;
            return true;
        }
        return false;
    }

    /** Is the target meaningfully off the ground (another flying dragon, phantom, etc.)? */
    private boolean isAirborne(LivingEntity target) {
        if (target instanceof TameableDragonEntity d && d.isFlying()) return true;
        // generic: not on the ground and clearly elevated (avoids tiny hops counting as flight)
        return !target.onGround() && target.fallDistance == 0.0F && !target.onClimbable();
    }

    @Override
    protected void doStart(ServerLevel level, TameableDragonEntity dragon) {
        super.doStart(level, dragon);
        dragon.setFlying(true);
        this.flyToAttackPosition(dragon);
    }

    @Override
    public void tickOrStop(ServerLevel level, TameableDragonEntity dragon, long time) {
        LivingEntity target = dragon.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
        if (target == null || !target.isAlive()
                || dragon.isBeingRiddenByPlayer()
                || dragon.distanceToSqr(target) > ENGAGE_RANGE_SQR
                || !this.shouldFly(dragon, target)) {
            this.doStop(level, dragon, time);
            return;
        }
        dragon.setFlying(true);
        // keep the target as the look target so the dragon orients toward it between breaths
        dragon.getBrain().setMemory(MemoryModuleType.LOOK_TARGET,
                new net.minecraft.world.entity.ai.behavior.EntityTracker(target, true));

        // While actually breathing, YIELD positioning to DragonBreathAttack: hold the current
        // spot (stay airborne, no reposition) so its faceTarget aim isn't overridden by the move
        // control rotating us toward a flight heading. Reposition only between breaths.
        if (dragon.isBreathing()) {
            dragon.getMoveControl().setWantedPosition(dragon.getX(), dragon.getY(), dragon.getZ(), 0.2);
        } else {
            this.flyToAttackPosition(dragon);
        }
    }

    /** Fly to a standoff point above/beside the target, within breath range. */
    private void flyToAttackPosition(TameableDragonEntity dragon) {
        LivingEntity target = dragon.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
        if (target == null) return;

        // vector from target to dragon, flattened, used to keep a standoff distance
        double dx = dragon.getX() - target.getX();
        double dz = dragon.getZ() - target.getZ();
        double horiz = Math.sqrt(dx * dx + dz * dz);
        double nx, nz;
        if (horiz < 1.0E-4) {
            // directly above/below: pick an arbitrary horizontal direction to back off to
            nx = 1.0;
            nz = 0.0;
        } else {
            nx = dx / horiz;
            nz = dz / horiz;
        }

        double wantX = target.getX() + nx * STANDOFF;
        double wantZ = target.getZ() + nz * STANDOFF;
        // hold above the target so the breath rains down and ground melee can't easily reach us
        double wantY = Math.max(target.getY() + STRAFE_HEIGHT, target.getEyeY() + STRAFE_HEIGHT);

        dragon.getMoveControl().setWantedPosition(wantX, wantY, wantZ, FLIGHT_SPEED);
    }

    @Override
    public void doStop(ServerLevel level, TameableDragonEntity dragon, long time) {
        super.doStop(level, dragon, time);
        // don't force-land; the dragon's own aiStep flight logic will settle it back down once it
        // stops being driven upward. Just release our commitment.
        this.committedAir = false;
    }
}
