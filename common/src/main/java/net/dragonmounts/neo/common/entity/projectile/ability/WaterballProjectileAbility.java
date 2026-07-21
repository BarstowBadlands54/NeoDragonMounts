package net.dragonmounts.neo.common.entity.projectile.ability;

import net.dragonmounts.neo.common.entity.dragon.TameableDragonEntity;
import net.dragonmounts.neo.common.entity.projectile.entity.DragonIceballEntity;
import net.dragonmounts.neo.common.entity.projectile.entity.DragonWaterballEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

public class WaterballProjectileAbility extends DragonProjectileAbility {
    private final int explosionPower;

    public WaterballProjectileAbility(int minChargeTicks, int cooldownTicks, int explosionPower) {
        super(minChargeTicks, cooldownTicks);
        this.explosionPower = explosionPower;
    }

    @Override
    public void fire(TameableDragonEntity dragon, ServerLevel level, Vec3 origin, Vec3 aim, float power) {
        // movement is the aim scaled; LargeFireball normalizes internally in 1.21.1
        Vec3 movement = aim.scale((1.0 + power) * 12);     // faster when fully charged
        DragonWaterballEntity ball = new DragonWaterballEntity(level, dragon, movement);
        ball.setPos(origin.x, origin.y, origin.z);
        level.addFreshEntity(ball);
    }
}