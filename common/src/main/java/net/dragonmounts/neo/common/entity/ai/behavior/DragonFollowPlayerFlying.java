package net.dragonmounts.neo.common.entity.ai.behavior;

import net.dragonmounts.neo.common.entity.dragon.ServerDragonEntity;
import net.dragonmounts.neo.common.entity.dragon.TameableDragonEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

/// Keeps an owner's dragons flying in a V behind them while they elytra or ride, and swoops in
/// to catch the owner when they fall.
public class DragonFollowPlayerFlying extends GoalBehavior<TameableDragonEntity> {
    /// Within this of its slot the dragon velocity-matches; beyond it, it sprints in.
    private static final double FORMATION_RANGE = 12.0;
    /// Past this the dragon is teleported back into formation instead of chasing.
    private static final double REJOIN_RANGE = 64.0;
    private static final double FOLLOW_RANGE = 35.0;

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

            if(!dragon.isBreakInTrusted()) {
                return false;
            }

            if (dragon.isBaby()) {
                return false;
            }
            boolean ownerFlyingMount = owner.getVehicle() instanceof ServerDragonEntity mount && mount.isFlying() && mount.isTame();
            return owner.isFallFlying() || ownerFlyingMount || owner.fallDistance > 4;
        }
        return false;
    }

    @Override
    public void tickOrStop(ServerLevel level, TameableDragonEntity dragon, long gameTime) {
        LivingEntity owner = dragon.getOwner();
        if (owner == null) return;

        dragon.setFlying(true);

        double dist = dragon.distanceTo(owner);

        if (owner.getVehicle() instanceof ServerDragonEntity dragonEntity) {
            dragon.setSprinting(dragonEntity.isSprinting());
        }
        // owner is elytra-flying or riding a flying vehicle: take up a slot in the V
        if (owner.isFallFlying() || (owner.getVehicle() instanceof ServerDragonEntity dragonEntity && (dragonEntity.isFlying() || dragon.isSwimming()))) {
            Vec3 slot = computeFormationSlot(level, dragon, owner);

            double gap = dragon.position().distanceTo(slot);

            // what the whole formation is travelling at
            Vec3 ownerVel = owner.getVehicle() != null
                    ? owner.getVehicle().getDeltaMovement()
                    : owner.getDeltaMovement();

            // a dragon left far behind (a long firework boost, say) rejoins rather than chases
            if (gap > REJOIN_RANGE) {
                dragon.moveTo(slot.x, slot.y, slot.z, dragon.getYRot(), dragon.getXRot());
                dragon.setDeltaMovement(ownerVel);   // inherit momentum so it cruises, not stalls
                return;
            }

            // Cruise or sprint is decided on distance, not speed. A speed-based cutoff leaves a
            // band where the dragon reaches its slot but the slot is not fleeing fast enough, and
            // it oscillates around the point.
            if (gap <= FORMATION_RANGE) {
                // Fly at the owner's velocity plus a proportional pull toward the slot. Moving
                // with the formation makes the slot near-stationary relative to the dragon, so
                // there is nothing to overshoot and the heading never flips.
                Vec3 toSlot = slot.subtract(dragon.position());
                double k = Math.min(0.35, 0.12 + gap * 0.03);
                Vec3 desired = ownerVel.add(toSlot.scale(k));
                Vec3 dm = dragon.getDeltaMovement().scale(0.55).add(desired.scale(0.45));
                dragon.setDeltaMovement(dm);
                dragon.getNavigation().stop();

                // Face the owner's heading, never the dragon's own motion: owner yaw is smooth.
                float faceYaw = (owner.getVehicle() != null ? owner.getVehicle() : owner).getYRot();
                float cur = dragon.getYRot();
                float newYaw = cur + net.minecraft.util.Mth.wrapDegrees(faceYaw - cur) * 0.25F;
                dragon.setYRot(newYaw);
                dragon.yBodyRot = dragon.yHeadRot = newYaw;
                return;
            }

            // far from the slot: sprint in and let DragonMoveControl damp the arrival
            double speed = Math.min(14.0, 6.5 + gap * 0.35);
            dragon.getMoveControl().setWantedPosition(slot.x, slot.y, slot.z, speed);
            return;
        }

        // owner is falling without an elytra: swoop in and catch them
        if (owner.fallDistance > 4 && !owner.onGround()) {
            if (dist < FOLLOW_RANGE) {
                if (dist <= dragon.getBbWidth() * 1.4 || dist <= dragon.getBbHeight()) {
                    if (!owner.isShiftKeyDown()) {
                        owner.startRiding(dragon);
                        return;
                    }
                }
                dragon.getMoveControl().setWantedPosition(
                        owner.getX(),
                        owner.getY() - 2.0,   // below the owner, to intercept the fall
                        owner.getZ(),
                        2.0
                );
            }
        }
    }

    /**
     * This dragon's slot in the V. The owner is the apex and dragons fill alternating wings,
     * each further back and further out. Assignment is deterministic, so they do not fight over
     * positions.
     */
    private static Vec3 computeFormationSlot(ServerLevel level, TameableDragonEntity dragon, LivingEntity owner) {
        // Generous radius: a dragon that is genuinely following must not flicker in and out of
        // the flock at slow speeds, or every rank re-shuffles.
        AABB area = owner.getBoundingBox().inflate(96.0);
        List<TameableDragonEntity> flock = level.getEntitiesOfClass(
                TameableDragonEntity.class, area,
                d -> !d.isBaby()
                        && d.getControllingPassenger() == null
                        && d.getVehicle() == null
                        && owner.equals(d.getOwner())
        );
        // A player-set flight rank wins and survives reloads; dragons left on auto fall back to
        // age and sit behind the ranked ones. Entity id is the final tiebreaker.
        flock.sort(
                Comparator.<TameableDragonEntity>comparingInt(d -> {
                            int r = d.getFlightRank();
                            return r > 0 ? r : Integer.MAX_VALUE;   // unset sorts to the back
                        })
                        .thenComparingInt(d -> -d.tickCount)         // among unset, oldest first
                        .thenComparingInt(d -> d.getId())
        );

        // The slot comes from the post-sort index, never the raw rank: two dragons sharing a
        // rank still get distinct indices, so they cannot stack on the same point in the V.
        int index = flock.indexOf(dragon);
        if (index < 0) index = 0;

        // Orientation always comes from body yaw, never velocity: velocity wobbles enough at
        // speed to spin the whole formation, yaw changes smoothly.
        var headingEntity = owner.getVehicle() != null ? owner.getVehicle() : owner;
        float yawRad = headingEntity.getYRot() * ((float) Math.PI / 180.0F);
        Vec3 forward = new Vec3(-Math.sin(yawRad), 0.0, Math.cos(yawRad));
        if (forward.lengthSqr() < 1.0E-4) forward = new Vec3(0.0, 0.0, 1.0);
        forward = forward.normalize();
        Vec3 right = new Vec3(-forward.z, 0.0, forward.x);

        int rank = index / 2 + 1;
        boolean leftSide = (index % 2) == 0;

        // Spaced by body size so large dragons do not overlap. bbWidth is the wingless width, so
        // both figures are widened for wingspan.
        double bodySize = Math.max(dragon.getBbWidth(), dragon.getScale() * 2.0);
        double back = bodySize * 3.0 * rank;
        double side = bodySize * 4.5 * rank * (leftSide ? -1.0 : 1.0);

        double tx = owner.getX() - forward.x * back + right.x * side;
        double ty = owner.getY() + 0.5;   // just above the owner's line, for visibility
        double tz = owner.getZ() - forward.z * back + right.z * side;
        return new Vec3(tx, ty, tz);
    }
}