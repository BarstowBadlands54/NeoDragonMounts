package net.dragonmounts.neo.common.entity.ai.navigation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.Vec3;

public class HybridDragonNavigator extends PathNavigation {

    private final GroundPathNavigation groundNav;
    private final RRTStarNavigator airNav;
    private boolean flying = false;

    public HybridDragonNavigator(Mob mob, Level level) {
        super(mob, level);
        this.groundNav = new GroundPathNavigation(mob, level);
        this.airNav = new RRTStarNavigator(mob, level);
    }

    /**
     * Properly initializes the evaluator used by the base class.
     */
    @Override
    protected PathFinder createPathFinder(int range) {
        WalkNodeEvaluator evaluator = new WalkNodeEvaluator();
        evaluator.setCanPassDoors(true);
        evaluator.setCanOpenDoors(true);
        evaluator.setCanFloat(true); // important for water/edge walking
        // Base class stores this internally, no need to assign manually.
        return new PathFinder(evaluator, range);
    }

    @Override
    protected Vec3 getTempMobPos() {
        return this.mob.position();
    }

    @Override
    protected boolean canUpdatePath() {
        return true;
    }

    @Override
    public void tick() {
        boolean shouldFly = shouldFly();
        if (shouldFly != this.flying) {
            this.flying = shouldFly;
            this.mob.setNoGravity(flying);
            this.groundNav.stop();
            this.airNav.stop();
        }

        if (flying) {
            this.airNav.tick();
            this.path = this.airNav.getPath();
        } else {
            this.groundNav.tick();
            this.path = this.groundNav.getPath();
        }

        logDebug();
    }

    private boolean shouldFly() {
        if (this.mob.onGround()) return false;
        BlockPos pos = BlockPos.containing(this.mob.position());
        int groundY = this.mob.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ());
        return this.mob.getY() - groundY > 2.0;
    }

    @Override
    public boolean moveTo(double x, double y, double z, double speed) {
        boolean result = this.flying
                ? this.airNav.moveTo(x, y, z, speed)
                : this.groundNav.moveTo(x, y, z, speed);

        this.path = this.flying ? this.airNav.getPath() : this.groundNav.getPath();
        return result;
    }

    @Override
    public void stop() {
        this.groundNav.stop();
        this.airNav.stop();
        this.path = null;
    }

    @Override
    public boolean isDone() {
        return this.flying ? this.airNav.isDone() : this.groundNav.isDone();
    }

    private void logDebug() {
        Path p = this.flying ? this.airNav.getPath() : this.groundNav.getPath();
        System.out.printf(
                "[HybridNav] flying=%s path=%s done=%s pos=(%.2f, %.2f, %.2f)%n",
                flying,
                p == null ? "null" : "ok(" + p.getNodeCount() + ")",
                isDone(),
                mob.getX(), mob.getY(), mob.getZ()
        );
    }
}
