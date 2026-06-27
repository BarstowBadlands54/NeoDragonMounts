package net.dragonmounts.neo.common.entity.ai.control;

import net.dragonmounts.neo.common.entity.dragon.TameableDragonEntity;
import net.dragonmounts.neo.common.util.math.MathUtil;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.control.MoveControl;

public class DragonMoveControl extends MoveControl {
    public final TameableDragonEntity dragon;

    public DragonMoveControl(TameableDragonEntity dragon) {
        super(dragon);
        this.dragon = dragon;
    }

    /// @see FlyingMoveControl#tick()
    @Override
    public void tick() {
        var dragon = this.dragon;
        if (this.operation == Operation.MOVE_TO) {
            this.operation = Operation.WAIT;
            var pos = dragon.position();
            double distX = this.wantedX - pos.x, distY = this.wantedY - pos.y, distZ = this.wantedZ - pos.z;
            double squared = distX * distX + distZ * distZ;
            if (squared + distY * distY < 2.5E-7) {
                if (dragon.isFlying()) dragon.setYya(0.0F);
                dragon.setZza(0.0F);
                return;
            }

            // ============ WATER FIRST — before onGround/air, so  depth never matters ============
            // inside MOVE_TO, immediately after the "close enough" early-return:
            // inside MOVE_TO, immediately after the "close enough" early-return:
            if (dragon.isInWater()) {
                // smooth swim toward target — same slow speed regardless of floor below
                float yaw = (float) (Mth.atan2(distZ, distX) * Mth.RAD_TO_DEG) - 90.0F;
                dragon.setYRot(this.rotlerp(dragon.getYRot(), yaw, 10.0F));
                dragon.yBodyRot = dragon.getYRot();

                float speed = (float) (this.speedModifier * dragon.getAttributeValue(Attributes.MOVEMENT_SPEED));
                dragon.setSpeed(speed);

                double horiz = Math.sqrt(squared);
                if (Math.abs(distY) > 1.0E-5 || horiz > 1.0E-5) {
                    float pitch = -(float) (Mth.atan2(distY, horiz) * Mth.RAD_TO_DEG);
                    pitch = Mth.clamp(Mth.wrapDegrees(pitch), -75.0F, 75.0F);
                    dragon.setXRot(this.rotlerp(dragon.getXRot(), pitch, 5.0F));
                }
                // gentle vertical follow so it rises/dives toward the target smoothly
                dragon.setYya(distY > 0.0 ? speed : -speed);
                return;   // never fall into onGround/air branches
            }

            // ---- GROUND ----
            if (dragon.onGround()) {
                dragon.setYRot(this.rotlerp(
                        dragon.getYRot(),
                        (float) (Mth.atan2(distZ, distX) * 180.0F / MathUtil.PI) - 90.0F,
                        90.0F
                ));
                dragon.setSpeed((float) (this.speedModifier * dragon.getAttributeValue(Attributes.MOVEMENT_SPEED)));
                var location = dragon.blockPosition();
                var state = dragon.level().getBlockState(location);
                var shape = state.getCollisionShape(dragon.level(), location);
                if (distY > dragon.maxUpStep() && squared < Math.max(1.0F, dragon.getBbWidth())
                        || !shape.isEmpty()
                        && dragon.getY() < shape.max(Direction.Axis.Y) + location.getY()
                        && !state.is(BlockTags.DOORS)
                        && !state.is(BlockTags.FENCES)
                ) {
                    dragon.getJumpControl().jump();
                    this.operation = Operation.JUMPING;
                } else if (distY > 0.5F) {
                    dragon.setYya(dragon.yya + 0.5F);
                }
            }
            // ---- AIR (flight) ----
            else {
                double dist = Math.sqrt(squared);

                // ARRIVAL DAMPING: only turn toward the target and drive forward when we're
                // meaningfully far from it. When the dragon is basically ON its target (e.g. a
                // formation slot right next to it), turning toward that near point makes the
                // atan2 heading flip violently as the dragon drifts past — it overshoots, flips
                // ~180°, charges back, overshoots again. That back-and-forth IS the spinning.
                // So inside a small "brake radius" we hold heading and coast instead of chasing.
                final double BRAKE_RADIUS = 2.5;   // blocks; within this, stop re-aiming/charging
                final double ARRIVE_EPS   = 0.2;  // blocks; basically arrived -> coast only

                float baseSpeed = (float) (this.speedModifier * dragon.getAttributeValue(Attributes.FLYING_SPEED));

                if (dist < ARRIVE_EPS) {
                    // arrived: don't turn, don't thrust — just let momentum bleed off smoothly.
                    dragon.setSpeed(0.0F);
                    dragon.setZza(0.0F);
                    if (dragon.isFlying()) dragon.setYya(0.0F);
                } else {
                    // Scale speed down as we approach so we decelerate into the target instead
                    // of blowing through it (which is what caused the overshoot/flip oscillation).
                    double brakeFactor = Math.min(1.0, dist / BRAKE_RADIUS);
                    float speed = (float) (baseSpeed * brakeFactor);

                    // Only re-aim heading when far enough out that the heading is stable. Close in,
                    // keep the current yaw so a near, drifting target can't whip us around.
                    if (dist > BRAKE_RADIUS) {
                        dragon.setYRot(this.rotlerp(
                                dragon.getYRot(),
                                (float) (Mth.atan2(distZ, distX) * 180.0F / MathUtil.PI) - 90.0F,
                                30.0F
                        ));
                    }
                    dragon.yBodyRot = dragon.getYRot();

                    dragon.setSpeed(speed);
                    if (dist > Mth.EPSILON || Math.abs(distY) > Mth.EPSILON) {
                        dragon.setXRot(this.rotlerp(
                                dragon.getXRot(),
                                (float) (Mth.atan2(distY, dist) * -180.0F / MathUtil.PI),
                                85.0F
                        ));
                        // vertical thrust also damped by the same brake factor near arrival
                        dragon.setYya(distY > 0.0 ? speed : -speed);
                    }
                }
            }
        } else if (dragon.isInWater()) {
            // idle in water — stop cleanly, slight float
            dragon.setSpeed(0.0F);
            dragon.setYya(0.0F);
            dragon.setZza(0.0F);
        } else if (dragon.onGround()) {
            super.tick();
        } else if (this.operation == Operation.JUMPING) {
            this.operation = Operation.WAIT;
            dragon.setSpeed((float) (this.speedModifier * dragon.getAttributeValue(Attributes.FLYING_SPEED)));
        } else {
            dragon.setYya(0.0F);
            dragon.setZza(0.0F);
        }
    }
}