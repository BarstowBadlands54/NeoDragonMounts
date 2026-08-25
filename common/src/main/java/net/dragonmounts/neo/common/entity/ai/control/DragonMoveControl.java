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

            // Water is handled before ground and air, so depth never matters.
            if (dragon.isInWater()) {
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
                dragon.setYya(distY > 0.0 ? speed : -speed);
                return;
            }

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
            else {
                double dist = Math.sqrt(squared);
                float speed = (float) (this.speedModifier * dragon.getAttributeValue(Attributes.FLYING_SPEED));
                dragon.setSpeed(speed);
                // While breathing, DragonBreathAttack's faceTarget owns yaw and pitch, so do not
                // steer toward the flight heading; vertical thrust is still allowed, to hold
                // altitude.
                boolean breathing = dragon.isBreathing();
                if (!breathing) {
                    dragon.setYRot(this.rotlerp(
                            dragon.getYRot(),
                            (float) (Mth.atan2(distZ, distX) * 180.0F / MathUtil.PI) - 90.0F,
                            30.0F
                    ));
                }
                if (dist > Mth.EPSILON || Math.abs(distY) > Mth.EPSILON) {
                    if (!breathing) {
                        dragon.setXRot(this.rotlerp(
                                dragon.getXRot(),
                                (float) (Mth.atan2(distY, dist) * -180.0F / MathUtil.PI),
                                85.0F
                        ));
                    }
                    dragon.setYya(distY > 0.0 ? speed : -speed);
                }
            }
        } else if (dragon.isInWater()) {
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