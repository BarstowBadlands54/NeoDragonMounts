// File: RRTStarPlanner.java
package net.dragonmounts.neo.common.entity.ai.navigation;

import net.dragonmounts.neo.common.entity.ai.navigation.RRTStarNode;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.entity.Mob;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * RRT* planner for 3D flying navigation in Minecraft.
 *
 * This implementation is single-threaded and intended to run incrementally on the server
 * tick. It uses:
 *  - Euclidean distance as cost
 *  - Goal bias sampling
 *  - Local steering to a fixed step size
 *  - Collision checks by sampling the path using level.clip (raycast) and AABB checks
 *
 * Tuning:
 *  - MAX_ITERATIONS: number of samples per planning attempt
 *  - EPSILON: step size when extending tree
 *  - NEIGHBOR_RADIUS: radius for rewiring
 */
public class RRTStarPlanner {

    private final Level level;
    private final Mob mob;

    // Planner parameters (tune these)
    public int MAX_ITERATIONS = 800;           // per planning run
    public double EPSILON = 3.0;               // step length when extending (blocks)
    public double GOAL_BIAS = 0.15;            // probability to sample goal directly
    public double NEIGHBOR_RADIUS = 6.0;       // radius to consider for rewiring
    public double COLLISION_CHECK_STEP = 0.5;  // step when sampling segment for collisions
    public double NODE_CLEARANCE = 0.6;        // clearance radius for node (mob bounding box margin)

    private final Random rnd = ThreadLocalRandom.current();

    public RRTStarPlanner(Level level, Mob mob) {
        this.level = level;
        this.mob = mob;
    }

    /**
     * Plan a path from start to goal. Returns a list of Vec3 waypoints or null if failed.
     * This method runs up to MAX_ITERATIONS samples.
     */
    public List<Vec3> plan(Vec3 start, Vec3 goal) {
        RRTStarNode root = new RRTStarNode(start);
        List<RRTStarNode> tree = new ArrayList<>();
        tree.add(root);
        RRTStarNode bestGoalNode = null;
        double bestGoalCost = Double.POSITIVE_INFINITY;

        for (int iter = 0; iter < MAX_ITERATIONS; iter++) {
            Vec3 sample = randomSample(start, goal);
            RRTStarNode nearest = nearestNode(tree, sample);
            Vec3 newPos = steer(nearest.pos, sample, EPSILON);

            if (!isSegmentCollisionFree(nearest.pos, newPos)) continue;

            RRTStarNode newNode = new RRTStarNode(newPos);
            // connect to nearest, set cost
            newNode.parent = nearest;
            newNode.cost = nearest.cost + newPos.distanceTo(nearest.pos);
            nearest.children.add(newNode);

            // find neighbors within radius for rewiring
            List<RRTStarNode> neighbors = nearNodes(tree, newNode.pos, NEIGHBOR_RADIUS);
            // choose best parent among neighbors (including current parent)
            RRTStarNode bestParent = nearest;
            double bestCost = newNode.cost;

            for (RRTStarNode n : neighbors) {
                if (isSegmentCollisionFree(n.pos, newNode.pos)) {
                    double c = n.cost + n.pos.distanceTo(newNode.pos);
                    if (c < bestCost) {
                        bestParent = n;
                        bestCost = c;
                    }
                }
            }

            // attach to best parent
            if (bestParent != nearest) {
                // detach from nearest
                nearest.children.remove(newNode);
                bestParent.children.add(newNode);
                newNode.parent = bestParent;
                newNode.cost = bestCost;
            }

            // add to tree
            tree.add(newNode);

            // rewire neighbors to possibly use newNode as parent
            for (RRTStarNode n : neighbors) {
                if (n == newNode.parent) continue;
                if (isSegmentCollisionFree(newNode.pos, n.pos)) {
                    double newCost = newNode.cost + newNode.pos.distanceTo(n.pos);
                    if (newCost < n.cost) {
                        // rewire
                        if (n.parent != null) n.parent.children.remove(n);
                        newNode.children.add(n);
                        n.parent = newNode;
                        n.cost = newCost;
                        // Note: we don't recursively update descendants' costs here for performance;
                        // a full implementation should propagate cost changes recursively.
                        propagateCostToChildren(n);
                    }
                }
            }

            // check if new node reaches goal
            if (newNode.pos.distanceTo(goal) <= EPSILON && isSegmentCollisionFree(newNode.pos, goal)) {
                double goalCost = newNode.cost + newNode.pos.distanceTo(goal);
                if (goalCost < bestGoalCost) {
                    bestGoalCost = goalCost;
                    RRTStarNode goalNode = new RRTStarNode(goal);
                    goalNode.parent = newNode;
                    goalNode.cost = goalCost;
                    newNode.children.add(goalNode);
                    bestGoalNode = goalNode;
                    // optional: break early if good enough
                    if (goalCost < start.distanceTo(goal) * 1.05) break;
                }
            }
        }

        if (bestGoalNode == null) {
            // planning failed
            return null;
        }

        // reconstruct path
        List<Vec3> path = new ArrayList<>();
        RRTStarNode cur = bestGoalNode;
        while (cur != null) {
            path.add(cur.pos);
            cur = cur.parent;
        }
        Collections.reverse(path);
        // optional smoothing pass
        return smoothPath(path);
    }

    private void propagateCostToChildren(RRTStarNode node) {
        // update costs recursively for subtree rooted at node
        for (RRTStarNode child : node.children) {
            double expected = node.cost + node.pos.distanceTo(child.pos);
            if (child.cost != expected) {
                child.cost = expected;
                propagateCostToChildren(child);
            }
        }
    }

    private Vec3 randomSample(Vec3 start, Vec3 goal) {
        if (rnd.nextDouble() < GOAL_BIAS) {
            return goal;
        }
        // sample in a bounding box around start+goal midpoint with some padding
        Vec3 mid = start.add(goal).scale(0.5);
        double dx = Math.abs(goal.x - start.x) + 16;
        double dy = Math.abs(goal.y - start.y) + 12;
        double dz = Math.abs(goal.z - start.z) + 16;

        double sx = mid.x + (rnd.nextDouble() - 0.5) * dx * 2.0;
        double sy = Math.max(1.0, Math.min(level.getMaxY() - 1, mid.y + (rnd.nextDouble() - 0.5) * dy * 2.0));
        double sz = mid.z + (rnd.nextDouble() - 0.5) * dz * 2.0;
        return new Vec3(sx, sy, sz);
    }

    private RRTStarNode nearestNode(List<RRTStarNode> tree, Vec3 sample) {
        RRTStarNode best = null;
        double bestDist = Double.POSITIVE_INFINITY;
        for (RRTStarNode n : tree) {
            double d = n.pos.distanceTo(sample);
            if (d < bestDist) {
                bestDist = d;
                best = n;
            }
        }
        return best;
    }

    private Vec3 steer(Vec3 from, Vec3 to, double epsilon) {
        double dist = from.distanceTo(to);
        if (dist <= epsilon) return to;
        double t = epsilon / dist;
        return new Vec3(
                from.x + (to.x - from.x) * t,
                from.y + (to.y - from.y) * t,
                from.z + (to.z - from.z) * t
        );
    }

    private List<RRTStarNode> nearNodes(List<RRTStarNode> tree, Vec3 pos, double radius) {
        List<RRTStarNode> out = new ArrayList<>();
        double r2 = radius * radius;
        for (RRTStarNode n : tree) {
            if (n.pos.distanceToSqr(pos) <= r2) out.add(n);
        }
        return out;
    }

    /**
     * Collision check for a straight segment between a and b.
     * Uses raycasting and incremental AABB checks to ensure a flying mob's bounding box won't collide.
     */
    private boolean isSegmentCollisionFree(Vec3 a, Vec3 b) {
        // quick raycast
        ClipContext cc = new ClipContext(a, b, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mob);
        HitResult res = level.clip(cc);
        if (res.getType() != HitResult.Type.MISS) {
            return false;
        }

        // sample along the segment at COLLISION_CHECK_STEP intervals and check AABB collisions
        double dist = a.distanceTo(b);
        int steps = Math.max(2, (int) Math.ceil(dist / COLLISION_CHECK_STEP));
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            Vec3 p = lerp(a, b, t);
            if (!isPointFree(p)) return false;
        }
        return true;
    }

    private Vec3 lerp(Vec3 a, Vec3 b, double t) {
        return new Vec3(
                a.x + (b.x - a.x) * t,
                a.y + (b.y - a.y) * t,
                a.z + (b.z - a.z) * t
        );
    }

    /**
     * Basic point clearance check: create a small AABB around point and test for collisions with blocks.
     */
    private boolean isPointFree(Vec3 p) {
        // use mob bounding box size roughly, plus NODE_CLEARANCE margin
        double halfWidth = Math.max(0.6, mob.getBbWidth() / 2.0) + NODE_CLEARANCE;
        double halfHeight = Math.max(0.6, mob.getBbHeight() / 2.0) + NODE_CLEARANCE;
        AABB box = new AABB(p.x - halfWidth, p.y - halfHeight, p.z - halfWidth, p.x + halfWidth, p.y + halfHeight, p.z + halfWidth);
        // iterate blocks overlapped by box and check if solid
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
                    if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
                        // conservative: treat any collision shape as obstacle
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * Simple path smoothing — tries to shortcut between non-consecutive waypoints
     */
    /**
     * Smooths a raw RRT* path by shortcutting straight-line segments where possible.
     * Prevents infinite loops by advancing indices safely and limiting iterations.
     */
    public List<Vec3> smoothPath(List<Vec3> path) {
        if (path == null || path.size() <= 2) return path;

        List<Vec3> out = new ArrayList<>();
        out.add(path.get(0)); // always include start

        int i = 0;
        int safety = 0;
        final int maxSafety = path.size() * 4; // safety limit

        while (i < path.size() - 1 && safety++ < maxSafety) {
            int next = path.size() - 1;
            boolean found = false;

            // try to jump as far as possible forward
            for (int j = path.size() - 1; j > i + 1; j--) {
                if (isSegmentCollisionFree(path.get(i), path.get(j))) {
                    next = j;
                    found = true;
                    break;
                }
            }

            if (!found) {
                // no shortcut possible, step by one
                next = i + 1;
            }

            // avoid re-adding same index (safety)
            if (next <= i) {
                System.err.println("[RRT*] smoothPath stuck at index " + i);
                break;
            }

            out.add(path.get(next));
            i = next;
        }

        // ensure goal included
        if (!out.get(out.size() - 1).equals(path.get(path.size() - 1))) {
            out.add(path.get(path.size() - 1));
        }

        return out;
    }

}
