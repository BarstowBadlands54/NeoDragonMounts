package net.dragonmounts.neo.common.entity.ai.navigation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.FlyNodeEvaluator;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

/**
 * RRT* flying navigator for 1.20.4 (Mojang mappings).
 *
 * Implements the abstract PathNavigation methods required in 1.20.4:
 *  - createPathFinder(int)
 *  - getTempMobPos()
 *
 * Note: we still bypass the PathFinder for planning (we use RRT*), but returning
 * a valid PathFinder prevents abstract-method compile errors and keeps vanilla code
 * that expects a PathFinder from NPEing.
 */
public class RRTStarNavigator extends PathNavigation {

    public final RRTStarPlanner planner;
    public List<Vec3> currentPath = null;
    public int currentIndex = 0;
    public Vec3 targetPos = null;

    // steering parameters
    public double maxSpeed = 0.6;
    public double accel = 0.08;

    // collision settings
    public double COLLISION_CHECK_STEP = 0.5;
    public double NODE_CLEARANCE = 0.5;

    public RRTStarNavigator(Mob mob, Level level) {
        super(mob, level);
        this.planner = new RRTStarPlanner(level, mob);

        // default tuning (adjust to taste)
        this.planner.MAX_ITERATIONS = 600;
        this.planner.EPSILON = 3.5;
        this.planner.GOAL_BIAS = 0.12;
        this.planner.NEIGHBOR_RADIUS = 6.0;
        this.planner.NODE_CLEARANCE = 0.5;
        this.planner.COLLISION_CHECK_STEP = 0.5;
    }

    /**
     * Abstract in PathNavigation — return a PathFinder instance.
     * We select FlyNodeEvaluator so the vanilla pathfinder is configured for flying.
     * (We do not rely on PathFinder for our RRT* planning, but PathNavigation expects this.)
     */
    @Override
    protected PathFinder createPathFinder(int maxVisitedNodes) {
        FlyNodeEvaluator evaluator = new FlyNodeEvaluator();
        return new PathFinder(evaluator, maxVisitedNodes);
    }

    /**
     * Abstract in PathNavigation — provide temporary mob position used by some vanilla code.
     * Return the mob's current position.
     */
    @Override
    protected Vec3 getTempMobPos() {
        return this.mob.position();
    }

    @Override
    public boolean isDone() {
        return currentPath == null || currentIndex >= currentPath.size();
    }

    @Override
    public boolean canUpdatePath() {
        return true;
    }

    @Override
    public void tick() {
        if (this.targetPos == null) return;

        // If we have no path, try to plan once every few ticks
        if ((currentPath == null || currentIndex >= currentPath.size()) && this.mob.tickCount % 10 == 0) {
            Vec3 start = this.mob.position();
            Vec3 goal = this.targetPos;

            List<Vec3> path = planner.plan(start, goal);
            if (path != null && !path.isEmpty()) {
                this.currentPath = path;
                this.currentIndex = 0;
                System.out.println("[RRT*] path found: " + path.size());
            } else {
                System.out.println("[RRT*] path failed, fallback flight");
            }
        }

        if (currentPath == null || currentPath.isEmpty()) {
            // fallback direct flight
            this.mob.getMoveControl().setWantedPosition(targetPos.x, targetPos.y, targetPos.z, 1.0);
            return;
        }

        Vec3 waypoint = currentPath.get(currentIndex);
        double distToWp = waypoint.distanceTo(this.mob.position());

        if (distToWp < 1.0) {
            currentIndex++;
            if (currentIndex >= currentPath.size()) {
                currentPath = null;
                targetPos = null;
                return;
            }
            waypoint = currentPath.get(currentIndex);
        }

        Vec3 desired = waypoint.subtract(this.mob.position()).normalize().scale(maxSpeed);
        Vec3 currentVel = this.mob.getDeltaMovement();
        Vec3 steering = desired.subtract(currentVel);
        Vec3 newVel = currentVel.add(steering.scale(accel));

        if (newVel.length() > maxSpeed)
            newVel = newVel.normalize().scale(maxSpeed);

        this.mob.setDeltaMovement(newVel);

        // orient body
        float yaw = (float) (Math.atan2(newVel.z, newVel.x) * (180F / Math.PI)) - 90.0F;
        float pitch = (float) -(Math.atan2(newVel.y, Math.sqrt(newVel.x * newVel.x + newVel.z * newVel.z)) * (180F / Math.PI));
        this.mob.setYRot(rotlerp(this.mob.getYRot(), yaw, 10.0F));
        this.mob.setXRot(rotlerp(this.mob.getXRot(), pitch, 10.0F));
    }

    private float rotlerp(float current, float target, float maxDelta) {
        float diff = wrapDegrees(target - current);
        if (diff > maxDelta) diff = maxDelta;
        if (diff < -maxDelta) diff = -maxDelta;
        return current + diff;
    }

    private float wrapDegrees(float deg) {
        deg %= 360.0F;
        if (deg >= 180.0F) deg -= 360.0F;
        if (deg < -180.0F) deg += 360.0F;
        return deg;
    }

    @Override
    public boolean moveTo(double x, double y, double z, double speed) {
        this.targetPos = new Vec3(x, y, z);
        this.maxSpeed = Math.max(0.2, speed);
        this.currentPath = null;
        this.currentIndex = 0;
        return true;
    }

//    @Override
//    public boolean moveTo(Mob mob, double speed) {
//        // Unused in this system
//        return false;
//    }

    // ------------------------------------------------------------------------
    // Collision helpers (voxel-accurate swept AABB)
    // ------------------------------------------------------------------------

    public boolean isSegmentCollisionFree(Vec3 a, Vec3 b) {
        double dist = a.distanceTo(b);
        if (dist < 0.001) return isPointFree(a);

        int steps = Math.max(2, (int) Math.ceil(dist / COLLISION_CHECK_STEP));
        Vec3 step = b.subtract(a).scale(1.0 / steps);
        Vec3 p = a;

        for (int i = 0; i <= steps; i++) {
            if (!isPointFree(p)) return false;
            p = p.add(step);
        }
        return true;
    }

    public boolean isPointFree(Vec3 p) {
        double halfWidth = Math.max(0.6, mob.getBbWidth() / 2.0) + NODE_CLEARANCE;
        double halfHeight = Math.max(0.6, mob.getBbHeight() / 2.0) + NODE_CLEARANCE;

        AABB box = new AABB(
                p.x - halfWidth, p.y - halfHeight, p.z - halfWidth,
                p.x + halfWidth, p.y + halfHeight, p.z + halfWidth
        );

        int minX = (int) Math.floor(box.minX);
        int maxX = (int) Math.floor(box.maxX);
        int minY = (int) Math.floor(box.minY);
        int maxY = (int) Math.floor(box.maxY);
        int minZ = (int) Math.floor(box.minZ);
        int maxZ = (int) Math.floor(box.maxZ);

        for (int x = minX; x <= maxX; x++) {
            for (int y = Math.max(0, minY); y <= Math.min(level.getMaxY(), maxY); y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (state.isAir()) continue;

                    VoxelShape shape = state.getCollisionShape(level, pos);
                    if (!shape.isEmpty()) {
                        for (AABB voxelBox : shape.toAabbs()) {
                            AABB worldVoxel = voxelBox.move(pos);
                            if (box.intersects(worldVoxel)) {
                                return false;
                            }
                        }
                    }
                }
            }
        }
        return true;
    }
}
