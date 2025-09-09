package net.dragonmounts.neo.common.type;

import net.dragonmounts.neo.compat.registry.DragonType;
import net.dragonmounts.neo.compat.registry.DragonTypeBuilder;
import net.minecraft.resources.ResourceLocation;

public class EnchantedType extends DragonType {
    public EnchantedType(ResourceLocation identifier, DragonTypeBuilder builder) {
        super(identifier, builder);
    }
}
