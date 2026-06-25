package net.dragonmounts.neo.common.entity.ai.behavior;

import net.dragonmounts.neo.common.entity.dragon.TameableDragonEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

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

        // CASE 1: owner is elytra-flying -> follow alongside in the air
        if (owner.isFallFlying()) {
            // fly to a point near the owner; offset so the dragon doesn't sit exactly on them
            double offset = dragon.getScale() * 5.0;
            dragon.getMoveControl().setWantedPosition(
                    owner.getX() + offset,
                    owner.getY(),
                    owner.getZ() + offset,
                    1.5   // speed multiplier
            );
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
}
