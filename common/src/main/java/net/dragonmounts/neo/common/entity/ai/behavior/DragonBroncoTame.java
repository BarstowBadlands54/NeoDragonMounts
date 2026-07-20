//package net.dragonmounts.neo.common.entity.ai.behavior;
//
//import net.dragonmounts.neo.common.entity.dragon.TameableDragonEntity;
//import net.minecraft.server.level.ServerLevel;
//import net.minecraft.world.entity.player.Player;
//import net.minecraft.world.level.Level;
//import net.minecraft.world.phys.Vec3;
//import org.jetbrains.annotations.Nullable;
//
//public class DragonBroncoTame extends GoalBehavior<TameableDragonEntity> {
//
//    // --- Bronco-style taming (server-only state) ---
//    private int rideTicks = 0;
//
//    private int buckThreshold = 0;
//
//    private int breakProgress = 0;
//
//    public static final int RIDES_TO_TAME = 5;
//
//    public static final int SUCCESS_RIDE_TICKS = 60; // 3 seconds
//
//    public static final int BUCK_MIN_TICKS = 70;   // ~3.5s
//    public static final int BUCK_MAX_TICKS = 160;  // ~8s
//
//    public static final double BRONCO_CLIMB_HEIGHT = 40.0;
//
//    public static final double BRONCO_FLY_SPEED = 2.0;
//
//    public static final double BRONCO_SWEEP_RADIUS = 24.0;
//
//    private double breakInStartX = 0.0;
//    private double breakInStartY = 0.0;
//    private double breakInStartZ = 0.0;
//    private int ticksClimbY = 0;
//
//    @Nullable
//    private java.util.UUID breakingInPlayer = null;
//
//    // --- Saddle-less flight grace (a tamed dragon will fly briefly without a saddle, then insist on one) ---
//    private int noSaddleFlightTicks = 0;
//
//    public static final int NO_SADDLE_WARN_TICKS = 100;   // ~5s
//
//    public static final int NO_SADDLE_LAND_TICKS = 300;   // ~15s
//    private boolean noSaddleWarned = false;
//
//
//    @Override
//    protected boolean canUse(ServerLevel level, TameableDragonEntity entity) {
//        return false;
//    }
//
//    @Override
//    public void tickOrStop(ServerLevel level, TameableDragonEntity dragon, long time) {
//
//        // The player clinging on during a break-in (untamed dragon).
//        Player rider = dragon.getBreakInRider();
//        if (rider == null || (dragon.isBreakInTrusted() && isTame())) {
//            dragon.rideTicks = 0;
//            return;
//        }
//
//        // Safety: baby dragons can never be broken in.
//        if (dragon.isBaby()) {
//            dragon.ejectPassengers();
//            dragon.rideTicks = 0;
//            return;
//        }
//
//        // try to limit how the dragon flies too high
//        if (ticksClimbY < 120) {
//            ticksClimbY++;
//        }
//
//        // reset it to 0 so it starts flying high again when on ground;
//        if (onGround() && ticksClimbY > 0) {
//            ticksClimbY = 0;
//        }
//
//        // Drive the dragon's own flight controller to climb HIGH and weave around,
//        // exactly like DragonFollowPlayerFlying does — but toward a wild point far
//        // above the start, so the player is carried dangerously high.
//        dragon.setFlying(true);
//        dragon.setOrderedToSit(false);
//
//        // Anchor the climb at where the ride began (first tick records it).
//        if (dragon.rideTicks == 0) {
//            dragon.breakInStartX = dragon.getX();
//            dragon.breakInStartY = dragon.getY();
//            dragon.breakInStartZ = dragon.getZ();
//        }
//
//        // Target: high above, sweeping in WIDE arcs anchored to where the ride began
//        // (anchoring to the start — not the live position — makes it cover a large area
//        // instead of chasing its own tail in tight circles).
//        double climbHeight = ticksClimbY < 120 ? BRONCO_CLIMB_HEIGHT : 0;
//        double climbTarget = dragon.breakInStartY + climbHeight;
//        double t = dragon.tickCount * 0.12;                 // slower phase = broader, sweeping arcs
//        double sweepX = Math.sin(t) * BRONCO_SWEEP_RADIUS;
//        double sweepZ = Math.cos(t * 0.6) * BRONCO_SWEEP_RADIUS;  // different freq -> figure-8 / wandering path
//
//        // Stop any pathfinding/brain walk target from competing with our climb.
//        dragon.getNavigation().stop();
//        dragon.getBrain().eraseMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.WALK_TARGET);
//
//        dragon.getMoveControl().setWantedPosition(
//                dragon.breakInStartX + sweepX,
//                climbTarget,
//                dragon.breakInStartZ + sweepZ,
//                BRONCO_FLY_SPEED
//        );
//
//        ++dragon.rideTicks;
//
//        // Time to resolve the ride?
//        if (dragon.rideTicks >= dragon.buckThreshold) {
//            boolean success = dragon.rideTicks >= SUCCESS_RIDE_TICKS;
//            // Would dragon successful ride be the one that finally tames it?
//            boolean willTame = success
//                    && (dragon.breakProgress + 1) >= RIDES_TO_TAME
//                    && dragon.breakingInPlayer != null
//                    && rider.getUUID().equals(dragon.breakingInPlayer);
//
//            if (willTame) {
//                // BROKEN IN — accept the rider. Do NOT buck. Keep flying a victory lap.
//                ++dragon.breakProgress;
//                dragon.tame(rider);
//                dragon.setBreakInTrusted(true);
//                dragon.level().broadcastEntityEvent(dragon, ON_TAMING_SUCCEED); // hearts
//                dragon.rideTicks = 0;
//                // Dragon is tamed now; the rider keeps flying. The saddle-less flight
//                // timer (in TameableDragonEntity) will warn and eventually set them down.
//            } else {
//                dragon.buckOffRider(rider, success);
//            }
//        }
//    }
//
//    /**
//     * Throw the current rider off (a non-final outcome). If {@code success} the ride
//     * counted toward taming but didn't finish it; either way the dragon bucks and smokes.
//     */
//    private void buckOffRider(Player rider, boolean success, TameableDragonEntity dragon) {
//        Level level = dragon.level();
//
//        // Fling the player off with some momentum so they actually tumble.
//        Vec3 fling = dragon.getLookAngle().scale(-0.4).add(0.0, 0.3, 0.0);
//        dragon.ejectPassengers();
//        rider.setDeltaMovement(rider.getDeltaMovement().add(fling));
//        rider.hasImpulse = true;
//        rider.hurtMarked = true;
//
//        if (success) {
//            ++dragon.breakProgress;   // progress, but not the final ride
//        }
//        level.broadcastEntityEvent(dragon, ON_TAMING_FAIL); // smoke either way — it threw you
//
//        dragon.rideTicks = 0;
//        dragon.buckThreshold = BUCK_MIN_TICKS + dragon.getRandom().nextInt(BUCK_MAX_TICKS - BUCK_MIN_TICKS);
//    }
//}
