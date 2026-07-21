package net.dragonmounts.neo.common.init;

import net.dragonmounts.neo.common.entity.breath.BreathNodeEntity;
import net.dragonmounts.neo.common.entity.dragon.HatchableDragonEggEntity;
import net.dragonmounts.neo.common.entity.dragon.TameableDragonEntity;
import net.dragonmounts.neo.common.entity.projectile.entity.DragonChargeEntity;
import net.dragonmounts.neo.common.entity.projectile.entity.DragonIceballEntity;
import net.dragonmounts.neo.common.entity.projectile.entity.DragonWaterballEntity;
import net.dragonmounts.neo.compat.registry.EntityHolder;
import net.minecraft.world.entity.MobCategory;

import static net.dragonmounts.neo.compat.registry.EntityHolder.registerEntity;
import static net.dragonmounts.neo.compat.registry.EntityHolder.registerLivingEntity;

public class DMEntities {
    public static final EntityHolder<BreathNodeEntity> DRAGON_BREATH = registerEntity(
            "dragon_breath",
            MobCategory.MISC,
            BreathNodeEntity::new,
            builder -> builder.noSummon().sized(0.2F, 0.2F).clientTrackingRange(0)
    );
    public static final EntityHolder<HatchableDragonEggEntity> HATCHABLE_DRAGON_EGG = registerLivingEntity(
            "dragon_egg",
            MobCategory.MISC,
            HatchableDragonEggEntity::construct,
            null,
            builder -> builder.sized(0.875F, 1.0F).fireImmune()
    );
    public static final EntityHolder<TameableDragonEntity> TAMEABLE_DRAGON = registerLivingEntity(
            "dragon",
            MobCategory.CREATURE,
            TameableDragonEntity::construct,
            null,
            builder -> builder.sized(3.0F, 2.5F).fireImmune()
    );
    public static final EntityHolder<DragonChargeEntity> DRAGON_END_CHARGE = registerEntity(
            "dragon_end_charge",
            MobCategory.MISC,
            DragonChargeEntity::new,
            builder -> builder.sized(0.3125F, 0.3125F).clientTrackingRange(4).updateInterval(10)
    );
    public static final EntityHolder<DragonIceballEntity> DRAGON_ICE_CHARGE = registerEntity(
            "dragon_ice_charge",
            MobCategory.MISC,
            DragonIceballEntity::new,
            builder -> builder.sized(0.3125F, 0.3125F).clientTrackingRange(4).updateInterval(10)
    );
    public static final EntityHolder<DragonWaterballEntity> DRAGON_WATER_CHARGE = registerEntity(
            "dragon_water_charge",
            MobCategory.MISC,
            DragonWaterballEntity::new,
            builder -> builder.sized(0.3125F, 0.3125F).clientTrackingRange(4).updateInterval(10)
    );

    public static void init() {}
}
