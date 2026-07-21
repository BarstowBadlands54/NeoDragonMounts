package net.dragonmounts.neo.common.entity.projectile.ability;

import net.dragonmounts.neo.common.entity.dragon.TameableDragonEntity;
import net.dragonmounts.neo.common.entity.projectile.entity.DragonChargeEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

public class EnderChargeProjectileAbility extends DragonProjectileAbility {
    public EnderChargeProjectileAbility(int minChargeTicks, int cooldownTicks) {
        super(minChargeTicks, cooldownTicks);
    }

    @Override
    public void fire(TameableDragonEntity dragon, ServerLevel level, Vec3 origin, Vec3 aim, float power) {
        Vec3 movement = aim.scale((1.0 + power) * 12);
        DragonChargeEntity ball = new DragonChargeEntity(level, dragon, movement);
        ball.setPos(origin.x, origin.y, origin.z);
        level.addFreshEntity(ball);
        level.playSound(null, origin.x, origin.y, origin.z,
                SoundEvents.ENDER_DRAGON_SHOOT, SoundSource.HOSTILE, 1.0F, 1.0F);
    }
}