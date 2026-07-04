package net.dragonmounts.neo.common.type;

import net.dragonmounts.neo.common.entity.breath.DragonBreath;
import net.dragonmounts.neo.common.entity.breath.impl.WitherBreath;
import net.dragonmounts.neo.common.entity.dragon.TameableDragonEntity;
import net.dragonmounts.neo.common.init.DMSounds;
import net.dragonmounts.neo.common.init.DragonTypes;
import net.dragonmounts.neo.compat.registry.DragonTypeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;

public class WitherType extends SkeletonType {
    public WitherType(ResourceLocation identifier, DragonTypeBuilder builder) {
        super(identifier, builder);
    }

    @Override
    public DragonBreath initBreath(TameableDragonEntity dragon) {
        return new WitherBreath(dragon, 0.6F);
    }

    @Override
    public SoundEvent getAmbientSound(TameableDragonEntity dragon) {
        return dragon.isBaby() ? DMSounds.DRAGON_PURR_NETHER_HATCHLING : DMSounds.DRAGON_PURR_NETHER;
    }

    @Override
    public <T extends LivingEntity & Mutable> void onThunderHit(T entity, LightningBolt bolt) {
        super.onThunderHit(entity, bolt);
        convertByLightning(entity, DragonTypes.SKELETON);
    }
}
