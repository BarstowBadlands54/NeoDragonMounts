package net.dragonmounts.neo.common.type;

import net.dragonmounts.neo.common.entity.breath.DragonBreath;
import net.dragonmounts.neo.common.entity.breath.impl.SculkBreath;
import net.dragonmounts.neo.common.entity.dragon.TameableDragonEntity;
import net.dragonmounts.neo.common.init.DMSounds;
import net.dragonmounts.neo.compat.registry.DragonType;
import net.dragonmounts.neo.compat.registry.DragonTypeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public class SculkType extends DragonType {
    public SculkType(ResourceLocation identifier, DragonTypeBuilder builder) {
        super(identifier, builder);
    }

    @Override
    public DragonBreath initBreath(TameableDragonEntity dragon) {
        return new SculkBreath(dragon, 0.6F);
    }

    @Override
    public SoundEvent getAmbientSound(TameableDragonEntity dragon) {
        return dragon.isBaby() ? DMSounds.DRAGON_PURR_HATCHLING : DMSounds.DRAGON_PURR_ZOMBIE;
    }

    @Override
    public SoundEvent getDeathSound(TameableDragonEntity dragon) {
        return DMSounds.DRAGON_DEATH_ZOMBIE;
    }
}

