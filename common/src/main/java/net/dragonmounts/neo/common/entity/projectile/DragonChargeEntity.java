package net.dragonmounts.neo.common.entity.projectile;

import net.dragonmounts.neo.common.init.DMEntities;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.DragonFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class DragonChargeEntity extends DragonFireball {
    public DragonChargeEntity(EntityType<? extends DragonFireball> type, Level level) {
        super(type, level);
    }

    public DragonChargeEntity(Level level, LivingEntity owner, Vec3 movement) {
        super(DMEntities.DRAGON_CHARGE.cast(), level);   // OUR entity type, so onHit runs
        this.setOwner(owner);
        this.setRot(owner.getYRot(), owner.getXRot());
        this.setPos(owner.getX(), owner.getEyeY(), owner.getZ());
        this.setDeltaMovement(movement.normalize().scale(0.1));   // 0.1 = vanilla accelerationPower
    }

    @Override
    protected void onHit(HitResult result) {
        if (!this.level().isClientSide) {
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.GENERIC_EXPLODE.value(),   // ← loud, Holder needs .value()
                    SoundSource.HOSTILE, 4.0F, 1.0F);
        }
        super.onHit(result);
    }
}