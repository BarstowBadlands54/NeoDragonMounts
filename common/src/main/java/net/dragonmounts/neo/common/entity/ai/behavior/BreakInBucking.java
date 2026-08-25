package net.dragonmounts.neo.common.entity.ai.behavior;

import net.dragonmounts.neo.common.entity.dragon.ServerDragonEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import static net.dragonmounts.neo.common.entity.dragon.TameableDragonEntity.ON_TAMING_FAIL;
import static net.dragonmounts.neo.common.entity.dragon.TameableDragonEntity.ON_TAMING_SUCCEED;

/**
 * Break-in taming. While an unbroken dragon carries a rider it forces itself airborne, sweeps
 * around erratically, and throws the rider off after a randomised delay. Riders who hang on long
 * enough bank progress; {@link #RIDES_TO_TAME} banked rides tame the dragon.
 * <p>
 * Belongs to {@link net.minecraft.world.entity.schedule.Activity#CORE}: mounting sets
 * {@link net.dragonmounts.neo.common.init.DMMemories#IS_CONTROLLED}, which pins the brain to
 * the CONTROLLED activity, and nothing outside CORE runs while that is active.
 * <p>
 * State is per-instance, and a brain is built per entity, so each dragon banks its own progress.
 * Like the rest of the brain it is not persisted; a reload starts the count over.
 */
public class BreakInBucking extends GoalBehavior<ServerDragonEntity> {
    /** Banked rides needed to tame. */
    public static final int RIDES_TO_TAME = 5;
    /** Ticks a rider has to last for the ride to bank. */
    public static final int SUCCESS_RIDE_TICKS = 60;
    public static final int BUCK_MIN_TICKS = 70;
    public static final int BUCK_MAX_TICKS = 160;
    /** Blocks above the mount point the dragon climbs to. */
    public static final double CLIMB_HEIGHT = 40.0;
    /** Ticks spent climbing before it levels off back to the mount altitude. */
    public static final int CLIMB_TICKS = 120;
    public static final double FLY_SPEED = 2.0;
    /** Radius of the sweep, measured from the mount point rather than the live position. */
    public static final double SWEEP_RADIUS = 24.0;

    private static final double SWEEP_RATE = 0.12;
    /** The second axis runs slower, tracing a figure-eight instead of a circle. */
    private static final double SWEEP_Z_RATE = 0.6;

    /** Where the current ride began. The sweep is anchored here so it covers ground. */
    private Vec3 anchor = Vec3.ZERO;
    private int bankedRides;
    private int rideTicks;
    private int buckThreshold;
    private int climbTicks;

    @Override
    protected boolean canUse(ServerLevel level, ServerDragonEntity dragon) {
        return dragon.getBreakInRider() != null && !(dragon.isTame() && dragon.isBreakInTrusted());
    }

    @Override
    protected void doStart(ServerLevel level, ServerDragonEntity dragon) {
        super.doStart(level, dragon);
        this.anchor = dragon.position();
        this.rideTicks = 0;
        this.climbTicks = 0;
        this.buckThreshold = BUCK_MIN_TICKS + dragon.getRandom().nextInt(BUCK_MAX_TICKS - BUCK_MIN_TICKS);
    }

    @Override
    public void tickOrStop(ServerLevel level, ServerDragonEntity dragon, long time) {
        Player rider = dragon.getBreakInRider();
        if (rider == null || !this.canContinueToUse(level, dragon)) {
            this.doStop(level, dragon, time);
            return;
        }
        if (dragon.isBaby()) {
            dragon.ejectPassengers();
            this.doStop(level, dragon, time);
            return;
        }
        this.fly(dragon);
        if (++this.rideTicks < this.buckThreshold) return;
        boolean held = this.rideTicks >= SUCCESS_RIDE_TICKS;
        if (held) {
            ++this.bankedRides;
        }
        if (held && this.bankedRides >= RIDES_TO_TAME) {
            this.acceptRider(level, dragon, rider);
        } else {
            this.buckOff(level, dragon, rider);
        }
        this.doStop(level, dragon, time);
    }

    /**
     * Drive the dragon's own move control up and around the anchor. Navigation and the walk
     * target are cleared every tick so {@code MoveToTargetSink} cannot compete for the same
     * control.
     */
    private void fly(ServerDragonEntity dragon) {
        dragon.setFlying(true);
        dragon.setOrderedToSit(false);
        if (dragon.onGround()) {
            this.climbTicks = 0;
        } else if (this.climbTicks < CLIMB_TICKS) {
            ++this.climbTicks;
        }
        double phase = dragon.tickCount * SWEEP_RATE;
        double height = this.climbTicks < CLIMB_TICKS ? CLIMB_HEIGHT : 0.0;
        dragon.getNavigation().stop();
        dragon.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        dragon.getMoveControl().setWantedPosition(
                this.anchor.x + Math.sin(phase) * SWEEP_RADIUS,
                this.anchor.y + height,
                this.anchor.z + Math.cos(phase * SWEEP_Z_RATE) * SWEEP_RADIUS,
                FLY_SPEED
        );
    }

    /** Broken in. The rider keeps their seat and the dragon flies on. */
    private void acceptRider(ServerLevel level, ServerDragonEntity dragon, Player rider) {
        dragon.tame(rider);
        dragon.setBreakInTrusted(true);
        level.broadcastEntityEvent(dragon, ON_TAMING_SUCCEED);
    }

    /** Throw the rider clear with enough momentum to tumble. Mid-air, that is a real fall. */
    private void buckOff(ServerLevel level, ServerDragonEntity dragon, Player rider) {
        Vec3 fling = dragon.getLookAngle().scale(-0.4).add(0.0, 0.3, 0.0);
        dragon.ejectPassengers();
        rider.setDeltaMovement(rider.getDeltaMovement().add(fling));
        rider.hasImpulse = true;
        rider.hurtMarked = true;
        level.broadcastEntityEvent(dragon, ON_TAMING_FAIL);
    }
}
