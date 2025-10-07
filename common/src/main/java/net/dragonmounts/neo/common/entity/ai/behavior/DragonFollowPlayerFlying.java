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

    private int xDist;
    private int yDist;
    private int zDist;

    public DragonFollowPlayerFlying(TameableDragonEntity dragonBaseFlyingRideable, int xDist, int yDist, int zDist) {
        this.xDist = xDist;
        this.yDist = yDist;
        this.zDist = zDist;
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

            // if owner is on ground land next to owner
            return dragon.getOwner().isFallFlying();
        }
        return false;
    }

    @Override
    public void tickOrStop(ServerLevel level, TameableDragonEntity dragon, long gameTime) {
        LivingEntity target = dragon.getTarget();
        dragon.setFlying(true);
        LivingEntity owner = dragon.getOwner();
        if (target != null && dragon.distanceTo(target) < 8) {
            dragon.getNavigation().moveTo(target.getX(), target.getY(), target.getZ(), 4);
        } else {
            if (owner != null) {
                // don't catch if owner is too far away
                double followRange = 35;
                Vec3 movePos = new Vec3(owner.getX(), owner.getY() + yDist, owner.getZ());

                if (owner.fallDistance > 4 && !owner.isFallFlying()) {
                    if (dragon.distanceTo(owner) < followRange) {
                        // mount owner if close enough, otherwise move to owner
                        if (dragon.distanceTo(owner) <= dragon.getBbWidth() * 1.4 || dragon.distanceTo(owner) <= dragon.getBbHeight() * 1.0 && !owner.isShiftKeyDown() && dragon.isFlying()) {
                            owner.startRiding(dragon);
                        } else {
                            // y movement is too slow
                            dragon.getNavigation().moveTo(owner.getX(), owner.getY() - 5, owner.getZ(), 4F);
                        }
                    }
                }
            }
        }
    }
}
