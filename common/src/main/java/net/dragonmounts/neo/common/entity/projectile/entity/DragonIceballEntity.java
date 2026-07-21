package net.dragonmounts.neo.common.entity.projectile.entity;

import net.dragonmounts.neo.common.init.DMEntities;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class DragonIceballEntity extends AbstractHurtingProjectile {

    public DragonIceballEntity(EntityType<? extends AbstractHurtingProjectile> type, Level level) {
        super(type, level);
    }

    public DragonIceballEntity(Level level, LivingEntity owner, Vec3 movement) {
        super(DMEntities.DRAGON_ICE_CHARGE.cast(), level);
        this.setOwner(owner);
        this.setRot(owner.getYRot(), owner.getXRot());
        this.setPos(owner.getX(), owner.getEyeY(), owner.getZ());
        this.setDeltaMovement(movement.normalize().scale(0.1));

        this.level().playSound(
                null,
                this.getX(),
                this.getY(),
                this.getZ(),
                SoundEvents.POWDER_SNOW_PLACE,
                SoundSource.HOSTILE,
                0.8F,
                1.2F
        );
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);

        if (result.getType() != HitResult.Type.ENTITY || !this.ownedBy(((EntityHitResult) result).getEntity())) {

            if (!this.level().isClientSide) {

                ServerLevel serverLevel = (ServerLevel) this.level();

                // Ice explosion
                serverLevel.sendParticles(
                        ParticleTypes.SNOWFLAKE,
                        this.getX(),
                        this.getY(),
                        this.getZ(),
                        80,
                        0.7,
                        0.7,
                        0.7,
                        0.05
                );

                serverLevel.sendParticles(
                        ParticleTypes.ITEM_SNOWBALL,
                        this.getX(),
                        this.getY(),
                        this.getZ(),
                        25,
                        0.5,
                        0.5,
                        0.5,
                        0.10
                );

                serverLevel.playSound(
                        null,
                        this.getX(),
                        this.getY(),
                        this.getZ(),
                        SoundEvents.POWDER_SNOW_BREAK,
                        SoundSource.HOSTILE,
                        1.0F,
                        0.9F
                );

                List<LivingEntity> list = this.level().getEntitiesOfClass(
                        LivingEntity.class,
                        this.getBoundingBox().inflate(4.0, 2.0, 4.0)
                );

                AreaEffectCloud cloud = new AreaEffectCloud(
                        this.level(),
                        this.getX(),
                        this.getY(),
                        this.getZ()
                );

                Entity owner = this.getOwner();
                if (owner instanceof LivingEntity livingOwner) {
                    cloud.setOwner(livingOwner);
                }

                cloud.setParticle(ParticleTypes.SNOWFLAKE);
                cloud.setRadius(3.0F);
                cloud.setDuration(600);
                cloud.setRadiusPerTick((7.0F - cloud.getRadius()) / cloud.getDuration());
                cloud.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 1, 1));

                for (LivingEntity living : list) {
                    if (this.distanceToSqr(living) < 16.0D) {
                        cloud.setPos(living.getX(), living.getY(), living.getZ());
                        break;
                    }
                }

                this.level().addFreshEntity(cloud);
                this.discard();
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    protected ParticleOptions getTrailParticle() {
        return ParticleTypes.SNOWFLAKE;
    }

    @Override
    protected boolean shouldBurn() {
        return false;
    }
}