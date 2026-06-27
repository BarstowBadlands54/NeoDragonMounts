package net.dragonmounts.neo.common.type;

import net.dragonmounts.neo.common.entity.breath.DragonBreath;
import net.dragonmounts.neo.common.entity.breath.impl.AetherBreath;
import net.dragonmounts.neo.common.entity.dragon.TameableDragonEntity;
import net.dragonmounts.neo.compat.registry.DragonType;
import net.dragonmounts.neo.compat.registry.DragonTypeBuilder;
import net.minecraft.resources.ResourceLocation;

public class LightType extends DragonType {
    public LightType(ResourceLocation identifier, DragonTypeBuilder builder) {
        super(identifier, builder);
    }


    @Override
    public DragonBreath initBreath(TameableDragonEntity dragon) {
        return new AetherBreath(dragon, 0.7F);
    }
}
