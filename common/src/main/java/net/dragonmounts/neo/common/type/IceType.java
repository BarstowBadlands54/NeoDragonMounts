package net.dragonmounts.neo.common.type;

import net.dragonmounts.neo.common.entity.breath.DragonBreath;
import net.dragonmounts.neo.common.entity.breath.impl.IceBreath;
import net.dragonmounts.neo.common.entity.dragon.TameableDragonEntity;
import net.dragonmounts.neo.compat.registry.DragonType;
import net.dragonmounts.neo.compat.registry.DragonTypeBuilder;
import net.minecraft.resources.ResourceLocation;

public class IceType extends DragonType {
    public IceType(ResourceLocation identifier, DragonTypeBuilder builder) {
        super(identifier, builder);
    }

    @Override
    public DragonBreath initBreath(TameableDragonEntity dragon) {
        return new IceBreath(dragon, 0.7F);
    }
}
