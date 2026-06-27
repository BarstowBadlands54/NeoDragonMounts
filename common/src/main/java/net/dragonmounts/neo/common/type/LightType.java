package net.dragonmounts.neo.common.type;

import net.dragonmounts.neo.common.entity.breath.DragonBreath;
import net.dragonmounts.neo.common.entity.breath.impl.MoonlightBreath;
import net.dragonmounts.neo.common.entity.dragon.TameableDragonEntity;
import net.dragonmounts.neo.compat.registry.DragonType;
import net.dragonmounts.neo.compat.registry.DragonTypeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/**
 * Light dragon type. Part of the light family (alongside Moonlight/Sunlight); reuses the
 * moonlight breath since there is no dedicated Light breath. Habitat is high-altitude /
 * bright sky, mirroring the celestial theme.
 */
public class LightType extends DragonType {
    public LightType(ResourceLocation identifier, DragonTypeBuilder builder) {
        super(identifier, builder);
    }

    @Override
    public boolean isInHabitat(LivingEntity entity) {
        // bright, high places — top third of the world, exposed to sky
        return entity.level().canSeeSky(entity.blockPosition())
                && entity.getY() > entity.level().getHeight() * 0.66;
    }

    @Override
    public DragonBreath initBreath(TameableDragonEntity dragon) {
        return new MoonlightBreath(dragon, 0.7F);
    }
}