package net.dragonmounts.plus.compat.registry;

import net.dragonmounts.plus.compat.Dummy;
import net.minecraft.core.MappedRegistry;
import net.minecraft.resources.ResourceLocation;

public class CooldownCategory {
    public static final MappedRegistry<CooldownCategory> REGISTRY = Dummy.get();

    public final ResourceLocation identifier;

    public CooldownCategory(ResourceLocation identifier) {
        this.identifier = identifier;
    }

    public final int getId() {
        return Dummy.get();
    }
}
