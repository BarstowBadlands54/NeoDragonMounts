package net.dragonmounts.neo.common.entity.ai.behavior;

import net.dragonmounts.neo.common.entity.dragon.TameableDragonEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Forces the dragon to land automatically if it's floating or stuck mid-air.
 * Compatible with HybridDragonNavigator (1.21.4).
 */
public class DragonAutoLandGoal extends Goal {

    private final TameableDragonEntity dragon;
    private final double descendSpeed;
    private boolean landing;
    private int stuckTicks;

    public DragonAutoLandGoal(TameableDragonEntity dragon) {
        this.dragon = dragon;
        this.descendSpeed = dragon.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED) * 0.8;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        // Only if in air, not riding, not already landing
        return !dragon.onGround() && dragon.getControllingPassenger() == null;
    }

    @Override
    public boolean canContinueToUse() {
        return landing && !dragon.onGround();
    }

    @Override
    public void start() {
        landing = true;
        stuckTicks = 0;
        dragon.setNoGravity(false);
    }

    @Override
    public void tick() {
        Vec3 pos = dragon.position();
        Level level = dragon.level();

        double groundY = getGroundYBelow(pos);
        double diff = pos.y - groundY;

        if (diff > 1.0) {
            // Gradually descend
            if (dragon.getNavigation() != null) {
                dragon.getNavigation().moveTo(pos.x, groundY + 1.0, pos.z, descendSpeed);
            }
        } else {
            // Landed or near ground
            finishLanding();
        }

        // Failsafe: if stuck midair too long, force gravity on
        if (!dragon.onGround()) {
            stuckTicks++;
            if (stuckTicks > 100) {
                dragon.setNoGravity(false);
                finishLanding();
            }
        }
    }

    @Override
    public void stop() {
        finishLanding();
    }

    private void finishLanding() {
        landing = false;
        stuckTicks = 0;
        dragon.setNoGravity(false);
        if (dragon.getNavigation() != null) {
            dragon.getNavigation().stop();
        }
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
}
