package net.dragonmounts.neo.common.entity.ai.behavior;

import net.dragonmounts.neo.common.entity.dragon.TameableDragonEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

public class DragonFollowPlayerFlying extends GoalBehavior<TameableDragonEntity> {

    public DragonFollowPlayerFlying() {

    }

    @Override
    public boolean canUse(ServerLevel level, TameableDragonEntity dragon) {
        LivingEntity owner = dragon.getOwner();
        if (owner != null) {

            if (dragon.isLeashed()) {
                return false;
            }

            if (dragon.getControllingPassenger() != null || dragon.getVehicle() != null) {
                return false;
            }

            if(dragon.isBaby()) {
                return false;
            }

            // if owner is on ground land next to owner
            return dragon.getOwner().isFallFlying() || owner.fallDistance > 4;
        }
        return false;
    }

    @Override
    public void tickOrStop(ServerLevel level, TameableDragonEntity dragon, long gameTime) {
        LivingEntity owner = dragon.getOwner();
        if (owner == null) return;

        dragon.setFlying(true);

        double dist = dragon.distanceTo(owner);
        double followRange = 35.0;

        // CASE 1: owner is elytra-flying -> fly in a V FORMATION behind the owner, like birds.
        if (owner.isFallFlying()) {
            Vec3 slot = computeFormationSlot(level, dragon, owner);
            dragon.getMoveControl().setWantedPosition(slot.x, slot.y, slot.z, 1.5);
            return;
        }

        // CASE 2: owner is falling (not elytra) -> swoop in to catch
        if (owner.fallDistance > 4 && !owner.onGround()) {
            if (dist < followRange) {
                // close enough to mount -> catch the owner
                if (dist <= dragon.getBbWidth() * 1.4 || dist <= dragon.getBbHeight()) {
                    if (!owner.isShiftKeyDown()) {
                        owner.startRiding(dragon);
                        return;
                    }
                }
                // otherwise fly UNDER the owner to intercept the fall
                dragon.getMoveControl().setWantedPosition(
                        owner.getX(),
                        owner.getY() - 2.0,   // aim slightly below to catch
                        owner.getZ(),
                        2.0                    // fast swoop
                );
            }
        }
    }

    /**
     * Compute this dragon's slot in a V formation behind the elytra-flying owner.
     * The owner is the apex; dragons fill alternating left/right wings, each one further
     * back and further out — exactly like migrating birds. Slot assignment is stable
     * (sorted by entity id) so dragons don't fight over positions.
     */
    private static Vec3 computeFormationSlot(ServerLevel level, TameableDragonEntity dragon, LivingEntity owner) {
        // Gather all of the owner's elytra-following dragons in the area, sorted deterministically.
        AABB area = owner.getBoundingBox().inflate(48.0);
        List<TameableDragonEntity> flock = level.getEntitiesOfClass(
                TameableDragonEntity.class, area,
                d -> !d.isBaby()
                        && d.getControllingPassenger() == null
                        && d.getVehicle() == null
                        && owner.equals(d.getOwner())
        );
        flock.sort(Comparator.comparingInt(d -> d.getId()));

        int index = flock.indexOf(dragon);
        if (index < 0) index = 0;

        // Formation orientation: based on the owner's horizontal flight direction.
        Vec3 vel = owner.getDeltaMovement();
        Vec3 forward = new Vec3(vel.x, 0.0, vel.z);
        if (forward.lengthSqr() < 1.0E-4) {
            // owner barely moving horizontally — fall back to look direction
            Vec3 look = owner.getLookAngle();
            forward = new Vec3(look.x, 0.0, look.z);
            if (forward.lengthSqr() < 1.0E-4) forward = new Vec3(0.0, 0.0, 1.0);
        }
        forward = forward.normalize();
        // Right-hand perpendicular (so we can place left/right wings).
        Vec3 right = new Vec3(-forward.z, 0.0, forward.x);

        // Wing rank: 0 -> first pair, 1 -> second pair, ...
        int rank = index / 2 + 1;
        boolean leftSide = (index % 2) == 0;   // even -> left wing, odd -> right wing

        final double SPACING_BACK = dragon.getScale() * 4.0;  // how far each rank trails
        final double SPACING_SIDE = dragon.getScale() * 4.0;  // how far out each rank sits

        double back = SPACING_BACK * rank;
        double side = SPACING_SIDE * rank * (leftSide ? -1.0 : 1.0);

        // Target = owner position, minus forward*back (behind), plus right*side (out to the wing).
        double tx = owner.getX() - forward.x * back + right.x * side;
        double ty = owner.getY() + 0.5;   // sit just above the owner's line for visibility
        double tz = owner.getZ() - forward.z * back + right.z * side;
        return new Vec3(tx, ty, tz);
    }
}