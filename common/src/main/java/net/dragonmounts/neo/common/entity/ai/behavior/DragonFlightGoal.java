package net.dragonmounts.neo.common.entity.ai.behavior;

import net.dragonmounts.neo.common.entity.dragon.TameableDragonEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Makes the dragon fly for a short period, then land automatically.
 * Works with HybridDragonNavigator (1.21.4).
 */
public class DragonFlightGoal extends Goal {

    private final TameableDragonEntity dragon;
    private final double flySpeed;
    private final double landSpeed;
    private int flightTicks;
    private int maxFlightTicks;
    private boolean landing;

    public DragonFlightGoal(TameableDragonEntity dragon) {
        this.dragon = dragon;

        // Use dragon's base movement speed attribute as a reference
        double baseSpeed = dragon.getAttributeValue(Attributes.MOVEMENT_SPEED);
        this.flySpeed = baseSpeed * 1.5;  // faster in the air
        this.landSpeed = baseSpeed * 0.8; // slower and smoother when landing

        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        // Only start flight if dragon is grounded and idle
        return this.dragon.onGround() && this.dragon.getRandom().nextInt(300) == 0;
    }

    @Override
    public void start() {
        this.flightTicks = 0;
        this.maxFlightTicks = 2000 + this.dragon.getRandom().nextInt(100); // 5–10 seconds
        this.landing = false;
        this.dragon.setNoGravity(true);
        this.takeOff();
    }

    @Override
    public boolean canContinueToUse() {
        return !landing && flightTicks < maxFlightTicks;
    }

    @Override
    public void tick() {
        flightTicks++;

        // Random air wandering
        if (flightTicks % 20 == 0) {
            Vec3 target = getRandomAirTarget();
            if (target != null && this.dragon.getNavigation() != null) {
                this.dragon.getNavigation().moveTo(target.x, target.y, target.z, flySpeed);
            }
        }

        // Prepare to land
        if (flightTicks >= maxFlightTicks - 20) {
            this.startLanding();
        }

        // If floating low, start landing
        if (!landing && !dragon.onGround() && isFloatingTooLow()) {
            this.startLanding();
        }

        // Descend while landing
        if (landing) {
            Vec3 pos = dragon.position();
            double groundY = getGroundYBelow(pos);
            if (pos.y - groundY > 1.0) {
                if (this.dragon.getNavigation() != null) {
                    this.dragon.getNavigation().moveTo(pos.x, groundY + 1.0, pos.z, landSpeed);
                }
            } else {
                this.finishLanding();
            }
        }
    }

    @Override
    public void stop() {
        this.dragon.setNoGravity(false);
        this.landing = false;
    }

    /* --- Helpers --- */

    private void takeOff() {
        Vec3 pos = dragon.position();
        Vec3 target = pos.add(
                Mth.nextDouble(dragon.getRandom(), -6.0, 6.0),
                5.0 + Mth.nextDouble(dragon.getRandom(), 3.0, 6.0),
                Mth.nextDouble(dragon.getRandom(), -6.0, 6.0)
        );
        if (this.dragon.getNavigation() != null)
            this.dragon.getNavigation().moveTo(target.x, target.y, target.z, flySpeed);
    }

    private void startLanding() {
        if (!landing) {
            landing = true;
            this.dragon.setNoGravity(false);
        }
    }

    private void finishLanding() {
        landing = false;
        flightTicks = maxFlightTicks;
        this.dragon.setNoGravity(false);
        if (this.dragon.getNavigation() != null)
            this.dragon.getNavigation().stop();
    }

    private boolean isFloatingTooLow() {
        Vec3 pos = dragon.position();
        double groundY = getGroundYBelow(pos);
        return pos.y - groundY <= 2.0 && !dragon.onGround();
    }

    private double getGroundYBelow(Vec3 pos) {
        Level level = dragon.level();
        BlockPos.MutableBlockPos cursor = BlockPos.containing(pos).mutable();

        while (cursor.getY() > level.getMinY()) {
            BlockState state = level.getBlockState(cursor);
            if (state.blocksMotion() || state.isSolid()) {
                return cursor.getY() + 1.0;
            }
            cursor.move(0, -1, 0);
        }

        return level.getMinY();
    }

    private Vec3 getRandomAirTarget() {
        Vec3 base = dragon.position();
        double x = base.x + Mth.nextDouble(dragon.getRandom(), -16.0, 16.0);
        double y = base.y + Mth.nextDouble(dragon.getRandom(), 2.0, 10.0);
        double z = base.z + Mth.nextDouble(dragon.getRandom(), -16.0, 16.0);
        return new Vec3(x, y, z);
    }
}
