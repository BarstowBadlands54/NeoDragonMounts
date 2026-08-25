package net.dragonmounts.neo.common.entity.ai.behavior;

import net.dragonmounts.neo.common.entity.dragon.TameableDragonEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

/**
 * Aerial positioning for an unridden dragon in a fight. It neither breathes nor bites: it only
 * decides when the dragon should be in the air and flies it to a spot {@link DragonBreathAttack}
 * and {@code MeleeAttack} can work from.
 * <p>
 * It engages when the target is itself airborne, when the target is dangerous, or when the dragon
 * is hurt against a ground target. It releases when none of those hold, and the dragon settles
 * back to ground melee.
 */
public class DragonAerialCombat extends GoalBehavior<TameableDragonEntity> {
    /** Take to the air against a GROUND target once health drops below this fraction. */
    private static final float HURT_FRACTION = 0.70F;
    /** Return to ground combat against a ground target once healed back above this (hysteresis). */
    private static final float RECOVER_FRACTION = 0.85F;

    /** Engage from the air only while the target is within this horizontal-ish range. */
    private static final double ENGAGE_RANGE_SQR = 40.0 * 40.0;
    /**
     * Standoff and altitude against a GROUND target. Deliberately far: 16 out and 14 up puts the
     * dragon ~21 blocks away, outside a Warden's 15-block sonic-boom sphere -- which is the only
     * reach it has beyond melee, and which bypasses armour entirely, so distance is the only
     * defence against it. Still comfortably inside DragonBreathAttack's 32-block range, and the
     * shallower ~40-degree firing angle is far kinder to the neck than hovering right overhead.
     */
    private static final double GROUND_STANDOFF = 16.0;
    private static final double GROUND_HEIGHT = 14.0;
    /** Closer for an air duel: a flying foe can follow anyway, so standing off buys nothing. */
    private static final double AIR_STANDOFF = 12.0;
    private static final double AIR_HEIGHT = 6.0;
    /** Flight speed multiplier for the move control. */
    private static final double FLIGHT_SPEED = 1.3;

    /** Latched once committed to an aerial engagement, so the health hysteresis works. */
    private boolean committedAir = false;

    @Override
    protected boolean canUse(ServerLevel level, TameableDragonEntity dragon) {
        if (dragon.isBeingRiddenByPlayer()) return false;
        if (dragon.isBaby()) return false;
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

    /** Does this fight warrant being in the air? */
    private boolean shouldFly(TameableDragonEntity dragon, LivingEntity target) {
        // an airborne target cannot be engaged from the ground at all
        if (this.isAirborne(target)) {
            this.committedAir = true;
            return true;
        }

        // A dangerous ground foe is fought from the air from the outset, and the commitment is
        // latched so the dragon does not drop back down mid-fight. Waiting for the health trigger
        // below meant opening every fight by walking into melee.
        if (DragonBreathAttack.isDangerous(target)) {
            this.committedAir = true;
            return true;
        }

        // Hurt against a ground target. Hysteresis either side of the threshold, or the dragon
        // flaps up and down around it.
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

    /** Is the target meaningfully off the ground? The fallDistance test excludes tiny hops. */
    private boolean isAirborne(LivingEntity target) {
        if (target instanceof TameableDragonEntity d && d.isFlying()) return true;
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
        // look target, so the dragon orients toward the enemy between breaths
        dragon.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(target, true));
        // SetWalkTargetFromAttackTargetIfTargetOutOfReach sits later in FIGHT and points
        // WALK_TARGET at the enemy; MoveToTargetSink then feeds that to the move control and
        // overwrites the standoff below, dragging the dragon into melee between bursts.
        dragon.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);

        // While breathing, yield positioning to DragonBreathAttack and just hold the spot, or the
        // move control rotates the dragon off its aim. Reposition only between bursts.
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

        // flattened target-to-dragon vector, used to hold the standoff distance
        double dx = dragon.getX() - target.getX();
        double dz = dragon.getZ() - target.getZ();
        double horiz = Math.sqrt(dx * dx + dz * dz);
        double nx, nz;
        if (horiz < 1.0E-4) {
            // directly above or below, so any horizontal direction will do
            nx = 1.0;
            nz = 0.0;
        } else {
            nx = dx / horiz;
            nz = dz / horiz;
        }

        // a grounded foe cannot follow, so hold outside its reach; a flying one can, so do not
        boolean grounded = !this.isAirborne(target);
        double standoff = grounded ? GROUND_STANDOFF : AIR_STANDOFF;
        double height = grounded ? GROUND_HEIGHT : AIR_HEIGHT;

        double wantX = target.getX() + nx * standoff;
        double wantZ = target.getZ() + nz * standoff;
        double wantY = Math.max(target.getY() + height, target.getEyeY() + height);

        dragon.getMoveControl().setWantedPosition(wantX, wantY, wantZ, FLIGHT_SPEED);
    }

    @Override
    public void doStop(ServerLevel level, TameableDragonEntity dragon, long time) {
        super.doStop(level, dragon, time);
        // no forced landing: aiStep settles the dragon once it stops being driven upward
        this.committedAir = false;
    }
}
