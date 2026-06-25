package net.dragonmounts.neo.common.entity.projectile.ability;

import net.dragonmounts.neo.common.entity.dragon.TameableDragonEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

public abstract class DragonProjectileAbility {
    public final int minChargeTicks;
    public final int cooldownTicks;

    protected DragonProjectileAbility(int minChargeTicks, int cooldownTicks) {
        this.minChargeTicks = minChargeTicks;
        this.cooldownTicks = cooldownTicks;
    }

    public abstract void fire(TameableDragonEntity dragon, ServerLevel level, Vec3 origin, Vec3 aim, float power);
}