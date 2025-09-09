package net.dragonmounts.neo.common.type;

import net.dragonmounts.neo.common.entity.breath.DragonBreath;
import net.dragonmounts.neo.common.entity.breath.impl.NetherBreath;
import net.dragonmounts.neo.common.entity.dragon.TameableDragonEntity;
import net.dragonmounts.neo.common.init.DMSounds;
import net.dragonmounts.neo.compat.registry.DragonType;
import net.dragonmounts.neo.compat.registry.DragonTypeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public class NetherType extends DragonType {
    public NetherType(ResourceLocation identifier, DragonTypeBuilder builder) {
        super(identifier, builder);
    }

    @Override
    public DragonBreath initBreath(TameableDragonEntity dragon) {
        return new NetherBreath(dragon, 0.9F);
    }

    @Override
    public SoundEvent getAmbientSound(TameableDragonEntity dragon) {
        return dragon.isBaby() ? DMSounds.DRAGON_PURR_NETHER_HATCHLING : DMSounds.DRAGON_PURR_NETHER;
    }
}
