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

        // CASE 1: owner is elytra-flying or riding a flying vehicle -> fly in a V FORMATION behind the owner, like birds.
        if (owner.isFallFlying() || (owner.getVehicle() != null && !owner.onGround()))  {
            Vec3 slot = computeFormationSlot(level, dragon, owner);

            double gap = dragon.position().distanceTo(slot);

            // If the dragon has fallen way behind (e.g. during a long firework boost), blink it
            // back into formation so the V doesn't permanently break apart.
            if (gap > 64.0) {
                dragon.moveTo(slot.x, slot.y, slot.z, dragon.getYRot(), dragon.getXRot());
                // inherit the owner's (or their vehicle's) momentum so it cruises, not stalls
                Vec3 ownerVel = owner.getVehicle() != null
                        ? owner.getVehicle().getDeltaMovement()
                        : owner.getDeltaMovement();
                dragon.setDeltaMovement(ownerVel);
                return;
            }

            // Owner's horizontal speed (theirs, or their vehicle's if mounted).
            Vec3 ownerVel = owner.getVehicle() != null
                    ? owner.getVehicle().getDeltaMovement()
                    : owner.getDeltaMovement();
            double ownerSpeed = Math.sqrt(ownerVel.x * ownerVel.x + ownerVel.z * ownerVel.z);

            // DEADZONE: when the dragon is already near its slot AND the owner is hovering /
            // crawling, don't keep re-targeting a point right next to the dragon — that makes
            // the move control's atan2 heading whip around and the body spin. Instead just
            // glide to a gentle stop and face the SAME way the owner is heading.
            if (gap < 3.0 && ownerSpeed < 0.08) {
                dragon.getNavigation().stop();
                dragon.setDeltaMovement(dragon.getDeltaMovement().scale(0.8));   // ease to a hover
                // align facing with the owner's heading (or the formation's forward) — stable.
                float faceYaw = owner.getYRot();
                float cur = dragon.getYRot();
                dragon.setYRot(cur + net.minecraft.util.Mth.wrapDegrees(faceYaw - cur) * 0.2F);
                dragon.yBodyRot = dragon.getYRot();
                return;
            }

            // Adaptive speed: the further from the slot, the faster it flies to catch up,
            // then eases back to a steady cruise once it's in position. Base 2.5, up to ~8.0.
            double speed = Math.min(8.0, 2.5 + gap * 0.35);
            dragon.getMoveControl().setWantedPosition(slot.x, slot.y, slot.z, speed);
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
        // Gather all of the owner's elytra-following dragons. Use a generous radius so a
        // dragon that's genuinely following doesn't flicker in/out of the flock list at slow
        // speeds (which would make everyone re-shuffle ranks).
        AABB area = owner.getBoundingBox().inflate(96.0);
        List<TameableDragonEntity> flock = level.getEntitiesOfClass(
                TameableDragonEntity.class, area,
                d -> !d.isBaby()
                        && d.getControllingPassenger() == null
                        && d.getVehicle() == null
                        && owner.equals(d.getOwner())
        );
        // Order the flock. Dragons with a player-set flight rank (>0) use it directly —
        // this is fully stable and survives reloads. Dragons left on "auto" (rank 0) fall
        // back to age (tickCount, oldest first) and are placed after all manually-ranked ones.
        // Entity id is the final tiebreaker so the order is always deterministic.
        flock.sort(
                Comparator.<TameableDragonEntity>comparingInt(d -> {
                            int r = d.getFlightRank();
                            return r > 0 ? r : Integer.MAX_VALUE;   // unset -> sort to the back
                        })
                        .thenComparingInt(d -> -d.tickCount)         // among unset: oldest first
                        .thenComparingInt(d -> d.getId())
        );

        // Slot is derived from the POST-SORT index, never from the raw rank number. So even
        // if two dragons share the same rank (e.g. both set to 3, or a spawn-time collision),
        // the sort's tiebreakers give them distinct indices and they land in separate, adjacent
        // slots — they can never stack on the same point in the V.
        int index = flock.indexOf(dragon);
        if (index < 0) index = 0;

        // Formation orientation: based on the owner's horizontal flight direction. If the
        // owner is riding something (e.g. another dragon), use the VEHICLE's velocity, since
        // a passenger's own deltaMovement is usually zero.
        Vec3 vel = owner.getVehicle() != null ? owner.getVehicle().getDeltaMovement() : owner.getDeltaMovement();
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

        // Space the V by the dragon's actual body size so large dragons don't overlap.
        // bbWidth is the real wingless body width; widen generously for wingspan + breathing room.
        double bodySize = Math.max(dragon.getBbWidth(), dragon.getScale() * 2.0);
        final double SPACING_BACK = bodySize * 3.0;   // how far each rank trails
        final double SPACING_SIDE = bodySize * 4.5;   // how far out each rank sits (wider for wingspan)

        double back = SPACING_BACK * rank;
        double side = SPACING_SIDE * rank * (leftSide ? -1.0 : 1.0);

        // Target = owner position, minus forward*back (behind), plus right*side (out to the wing).
        double tx = owner.getX() - forward.x * back + right.x * side;
        double ty = owner.getY() + 0.5;   // sit just above the owner's line for visibility
        double tz = owner.getZ() - forward.z * back + right.z * side;
        return new Vec3(tx, ty, tz);
    }
}